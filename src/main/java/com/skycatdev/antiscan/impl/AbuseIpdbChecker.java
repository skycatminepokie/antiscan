package com.skycatdev.antiscan.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AbuseIpdbChecker implements ConnectionChecker {
    public static final File KEY_FILE = FabricLoader.getInstance().getGameDir().resolve(".antiscan-do-not-share").toFile();
    /**
     * Seconds
     */
    public static final long DEFAULT_UPDATE_DELAY = TimeUnit.HOURS.toSeconds(6);
    /**
     * UNIX epoch millis
     */
    public static final long DEFAULT_LAST_UPDATED = 0L;
    /**
     * Millis
     */
    public static final long REPORT_COOLDOWN = TimeUnit.MINUTES.toMillis(16);
    public static final MapCodec<AbuseIpdbChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("blacklist").forGetter(AbuseIpdbChecker::exportBlacklist),
            Codec.LONG.optionalFieldOf("last_updated", DEFAULT_LAST_UPDATED).forGetter(AbuseIpdbChecker::getLastUpdated),
            Codec.LONG.fieldOf("update_delay_seconds").orElse(DEFAULT_UPDATE_DELAY).forGetter(AbuseIpdbChecker::getUpdateDelay)
    ).apply(instance, AbuseIpdbChecker::new));
    public static final int ABUSE_CONFIDENCE_THRESHOLD = 90;
    /**
     * Locks all fields other than {@link AbuseIpdbChecker#reportTimes}
     */
    protected ReadWriteLock lock = new ReentrantReadWriteLock(true); // Fairness seems important when connections are waiting
    protected @Nullable String key;
    /**
     * Minimum time between updates, in seconds.
     */
    private long updateDelay;
    private long lastUpdated;
    /**
     * IPs that were not reported as abusive on AbuseIPDB
     */
    private transient HashSet<String> greylist;
    private HashSet<String> blacklist;
    /**
     * Ip -> last time reported (UNIX epoch millis)
     */
    private transient ConcurrentHashMap<String, Long> reportTimes;

    public AbuseIpdbChecker() {
        this(new HashSet<>(), DEFAULT_LAST_UPDATED, DEFAULT_UPDATE_DELAY);
    }

    public AbuseIpdbChecker(List<String> blacklist, long lastUpdated, long updateDelay) {
        this(new HashSet<>(blacklist), lastUpdated, updateDelay);
    }

    public AbuseIpdbChecker(HashSet<String> blacklist, long lastUpdated, long updateDelay) {
        this.blacklist = blacklist;
        key = loadKey();
        this.lastUpdated = lastUpdated;
        this.updateDelay = updateDelay;
        this.greylist = new HashSet<>();
        this.reportTimes = new ConcurrentHashMap<>();
    }

    protected static @Nullable String loadKey() {
        if (KEY_FILE.exists()) {
            if (KEY_FILE.isFile()) {
                try (Scanner scanner = new Scanner(KEY_FILE)) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (!line.isBlank() && !line.startsWith("#")) {
                            return line;
                        }
                    }
                } catch (IOException e) {
                    Antiscan.LOGGER.warn("Exception while trying to read AbuseIPDB key file. This is NOT a critical error.", e);
                }
            } else {
                Antiscan.LOGGER.warn("Cannot read AbuseIPDB key file - not a regular file");
            }
        } else {
            try {
                if (KEY_FILE.createNewFile()) {
                    try (PrintWriter writer = new PrintWriter(KEY_FILE)) {
                        writer.println("# Do not share!");
                        writer.println();
                    }
                    Antiscan.LOGGER.info("Created AbuseIPDB key file.");
                } else {
                    Antiscan.LOGGER.warn("Failed to create AbuseIPDB key file.");
                }
            } catch (IOException e) {
                Antiscan.LOGGER.warn("Failed to create AbuseIPDB key file.", e);
            }
        }
        return null;
    }

    public CompletableFuture<Boolean> report(String ip, Executor executor) {
        return CompletableFuture.supplyAsync(() -> reportNow(ip), executor);
    }

    public CompletableFuture<Boolean> report(Connection connection, Executor executor) {
        return CompletableFuture.supplyAsync(() -> reportNow(connection), executor);
    }

    /**
     * @return {@code true} if reported successfully or on cooldown, {@code false} otherwise
     */
    public boolean reportNow(String ip) {
        lock.readLock().lock();
        try {
            if (key == null) return false;
            long time = System.currentTimeMillis();
            AtomicBoolean shouldReport = new AtomicBoolean(false);
            reportTimes.compute(ip, (ipKey, prevTime) -> {
                if (prevTime == null || System.currentTimeMillis() + REPORT_COOLDOWN > prevTime) {
                    shouldReport.set(true);
                    return time;
                }
                return prevTime;
            });
            if (shouldReport.get()) {
                DataResult<HttpResponse<String>> request = Utils.sendHttpRequest(HttpRequest.newBuilder()
                        .uri(URI.create("https://api.abuseipdb.com/api/v2/report"))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                String.format("ip=%s&categories=%s&comment=%s",
                                        URLEncoder.encode(ip, StandardCharsets.UTF_8),
                                        URLEncoder.encode("14", StandardCharsets.UTF_8), // Category: port scanning
                                        URLEncoder.encode("Port scan to Minecraft server.", StandardCharsets.UTF_8))))
                        .setHeader("Key", key)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build(), HttpResponse.BodyHandlers.ofString());

                if (request.isSuccess()) return true;
                if (request.hasResultOrPartial()) {
                    Antiscan.LOGGER.warn("Failed to report IP to AbuseIPDB. Status code: {}", request.getPartialOrThrow().statusCode());
                    return false;
                }
                Antiscan.LOGGER.warn("Failed to report IP to AbuseIPDB.");
                return false;
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean reportNow(Connection connection) {
        if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
            return reportNow(socketAddress.getAddress().getHostAddress());
        }
        return false;
    }

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        CompletableFuture.runAsync(this::updateIfNeeded, executor);
        if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
            String ip = socketAddress.getAddress().getHostAddress();
            return CompletableFuture.supplyAsync(() -> {
                lock.readLock().lock();
                try {
                    if (blacklist.contains(ip)) return VerificationStatus.FAIL; // Quick check
                    if (greylist.contains(ip)) return VerificationStatus.PASS;
                } finally {
                    lock.readLock().unlock();
                }
                lock.writeLock().lock(); // We have no cache, check remote and write results
                try {
                    // Must also double-check state, in case state has changed while upgrading locks
                    if (blacklist.contains(ip))
                        return VerificationStatus.FAIL; // Writing is exclusive, we can read while writing.
                    if (greylist.contains(ip)) return VerificationStatus.PASS;
                    if (checkIpRemotely(ip)) {
                        blacklist.add(ip);
                        return VerificationStatus.FAIL;
                    } else {
                        greylist.add(ip);
                        return VerificationStatus.PASS;
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }, executor);
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    /**
     *
     * @return {@code true} if the ip is considered abusive.
     */
    private boolean checkIpRemotely(String ip) {
        lock.readLock().lock();
        try {
            if (key == null) return false;
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        try {
            if (key == null) return false;
            DataResult<HttpResponse<String>> request = Utils.sendHttpRequest(HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.abuseipdb.com/api/v2/check?ipAddress=%s", ip)))
                    .GET()
                    .setHeader("Key", key)
                    .setHeader("Accept", "application/json")
                    .timeout(Duration.of(5, TimeUnit.SECONDS.toChronoUnit()))
                    .build(), HttpResponse.BodyHandlers.ofString());

            if (request.isSuccess()) {
                HttpResponse<String> response = request.getOrThrow();
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                int abuseScore = json.get("data").getAsJsonObject().get("abuseConfidenceScore").getAsInt();
                if (abuseScore > ABUSE_CONFIDENCE_THRESHOLD) {
                    blacklist.add(ip);
                    return true;
                } else {
                    greylist.add(ip);
                    return false;
                }
            } else {
                if (request.hasResultOrPartial()) {
                    HttpResponse<String> failedResponse = request.getPartialOrThrow();
                    Antiscan.LOGGER.warn("Failed to load ip status from AbuseIPDB - got a non-2xx status code: {}. Response: {}",
                            failedResponse.statusCode(),
                            failedResponse.body());
                } else {
                    Antiscan.LOGGER.warn("Failed to load ip status from AbuseIPDB. Reason: {}", request.error().orElseThrow().message());
                }
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<String> exportBlacklist() {
        lock.readLock().lock();
        try {
            return new LinkedList<>(blacklist);
        } finally {
            lock.readLock().unlock();
        }
    }

    private long getLastUpdated() {
        lock.readLock().lock();
        try {
            return lastUpdated;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.ABUSE_IPDB;
    }

    private long getUpdateDelay() {
        lock.readLock().lock();
        try {
            return updateDelay;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return Success
     */
    @SuppressWarnings("UnusedReturnValue")
    protected boolean updateIfNeeded() {
        lock.readLock().lock();
        try {
            if (key == null || System.currentTimeMillis() < lastUpdated + updateDelay * 100) {
                return true;
            }
        } finally {
            lock.readLock().unlock();
        }
        // Time to promote
        lock.writeLock().lock();
        try {
            // May have changed since promoting lock
            if (key == null || System.currentTimeMillis() < lastUpdated + updateDelay * 100) {
                return true;
            }
            greylist.clear();
            blacklist.clear();

            lastUpdated = System.currentTimeMillis();
            DataResult<HttpResponse<String>> request = Utils.sendHttpRequest(HttpRequest.newBuilder()
                    .uri(URI.create("https://api.abuseipdb.com/api/v2/blacklist?confidenceMinimum=90"))
                    .GET()
                    .setHeader("Key", key)
                    .setHeader("Accept", "text/plain")
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (request.isSuccess()) {
                HttpResponse<String> response = request.getOrThrow();
                blacklist.addAll(Arrays.asList(response.body().split("\n")));
                Antiscan.LOGGER.info("Updated IP blacklist from AbuseIPDB.");
                return true;
            } else {
                if (request.hasResultOrPartial()) {
                    HttpResponse<String> failedResponse = request.getPartialOrThrow();
                    Antiscan.LOGGER.warn("Failed to load ip blacklist from AbuseIPDB - got a non-2xx status code: {}. Response: {}",
                            failedResponse.statusCode(),
                            failedResponse.body());
                } else {
                    Antiscan.LOGGER.warn("Failed to load ip blacklist from AbuseIPDB. Reason: {}", request.error().orElseThrow().message());
                }
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}
