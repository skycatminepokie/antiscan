package com.skycatdev.antiscan;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class IpChecker {
    public static final Codec<IpChecker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().xmap(IpChecker::listToConcurrentSet, set -> set.stream().toList()).fieldOf("blacklistCache").forGetter(IpChecker::getBlacklistCache),
            Codec.LONG.fieldOf("lastUpdated").forGetter(IpChecker::getLastUpdated),
            Codec.STRING.listOf().xmap(IpChecker::listToConcurrentSet, set -> set.stream().toList()).fieldOf("manualBlacklist").forGetter(IpChecker::getManualBlacklist),
            Codec.STRING.listOf().xmap(IpChecker::listToConcurrentSet, set -> set.stream().toList()).fieldOf("whitelistCache").forGetter(IpChecker::getWhitelistCache)
    ).apply(instance, (blacklistCache1, lastUpdated1, manualBlacklist1, whitelistCache1) -> new IpChecker(blacklistCache1, lastUpdated1, manualBlacklist1, whitelistCache1, new ConcurrentHashMap<>())));
    protected final Set<String> blacklistCache;
    protected final Set<String> manualBlacklist;
    protected final Set<String> whitelistCache;
    protected long lastUpdated;
    protected final transient Map<String, Long> reportingCache;
    protected final Lock fetchingUpdates = new ReentrantLock();

    protected IpChecker(Set<String> blacklistCache, long lastUpdated, Set<String> manualBlacklist, Set<String> whitelistCache, ConcurrentHashMap<String, Long> reportingCache) {
        this.blacklistCache = blacklistCache;
        this.lastUpdated = lastUpdated;
        this.manualBlacklist = manualBlacklist;
        this.whitelistCache = whitelistCache;
        this.reportingCache = reportingCache;
    }

    private static <T> @NotNull Set<T> listToConcurrentSet(List<T> list) {
        Set<T> set = ConcurrentHashMap.newKeySet();
        set.addAll(list);
        return set;
    }

    public static IpChecker load(File saveFile) throws IOException {
        AntiScan.LOGGER.info("Loading ip blacklist.");
        return Utils.loadFromFile(saveFile, CODEC);
    }

    public static IpChecker loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            AntiScan.LOGGER.info("Creating a new ip blacklist.");
            return new IpChecker(ConcurrentHashMap.newKeySet(), 0, ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), new ConcurrentHashMap<>());
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from save file. This is NOT a detrimental error.", e);
            return new IpChecker(ConcurrentHashMap.newKeySet(), 0, ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), new ConcurrentHashMap<>());
        }
    }

    public boolean blacklist(String ip) {
        return blacklist(ip, false);
    }

    public boolean blacklist(String ip, boolean manual) {
        try {
            return blacklist(ip, manual, null);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to save blacklist, even though we weren't trying?", e);
            return false;
        }
    }

    public boolean blacklist(String ip, boolean manual, @Nullable File saveFile) throws IOException {
        boolean added;
        if (manual) {
            added = manualBlacklist.add(ip);
        } else {
            added = blacklistCache.add(ip);
        }
        if (added && saveFile != null) {
            save(saveFile);
        }
        return added;
    }

    /**
     * @param ip The ip to check
     * @return {@code true} if the ip is deemed malicious, {@code false} otherwise (including a failure)
     */
    public boolean checkAbuseIpdb(String ip) {
        if (AntiScan.CONFIG.getAbuseIpdbKey() == null || whitelistCache.contains(ip) || AntiScan.IS_DEV_MODE) {
            return false;
        }
        AntiScan.LOGGER.info("Checking ip '{}'", ip);
        HttpResponse<String> response;
        //? if >=1.20.5
        try (HttpClient client = HttpClient.newHttpClient()) {
            //? if <1.20.5
            /*HttpClient client = HttpClient.newHttpClient();*/
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.abuseipdb.com/api/v2/check?ipAddress=%s", ip)))
                    .GET()
                    .setHeader("Key", AntiScan.CONFIG.getAbuseIpdbKey())
                    .setHeader("Accept", "application/json")
                    .timeout(Duration.of(5, TimeUnit.SECONDS.toChronoUnit()))
                    .build();
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB. This is NOT a fatal error.", e);
                return false;
            }
        //? if >=1.20.5
        }
        if (response != null) {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonElement json;
                try (JsonReader reader = new JsonReader(new StringReader(response.body()))) {
                    json = Streams.parse(reader);
                } catch (IOException e) {
                    AntiScan.LOGGER.warn("Failed to parse ip check from AbuseIPDB. This is NOT a fatal error.", e);
                    return true;
                }
                if (json != null) {
                    boolean safe = json.getAsJsonObject().get("data").getAsJsonObject().get("abuseConfidenceScore").getAsInt() < 90;
                    if (!safe) {
                        blacklistCache.add(ip);
                    } else {
                        whitelistCache.add(ip);
                    }
                    return safe;
                } else {
                    AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB - response was JSON null. This is NOT a fatal error.");
                    return false;
                }
            } else {
                AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB. This is NOT a fatal error. Status: {}. Body: {}", response.statusCode(), response.body());
                return false;
            }
        } else {
            AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB - response was null. This is NOT a fatal error.");
            return false;
        }
    }

    protected boolean fetchFromAbuseIpdb(String apiKey) {
        HttpResponse<String> response;
        // https://curlconverter.com/java/
        //? if >=1.20.5
        try (HttpClient client = HttpClient.newHttpClient()) {
        //? if <1.20.5
        /*HttpClient client = HttpClient.newHttpClient();*/
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.abuseipdb.com/api/v2/blacklist?confidenceMinimum=90"))
                    .GET()
                    .setHeader("Key", apiKey)
                    .setHeader("Accept", "text/plain")
                    .build();

            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                AntiScan.LOGGER.warn("Failed to load ip blacklist from AbuseIPDB. This is NOT a fatal error.", e);
                return false;
            }
        //? if >=1.20.5
        }
        if (response != null) {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                blacklistCache.addAll(Arrays.asList(response.body().split("\n")));
            } else {
                AntiScan.LOGGER.warn("Failed to load ip blacklist from AbuseIPDB. This is NOT a fatal error. Status: {}. Body: {}", response.statusCode(), response.body());
                return false;
            }
        } else {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from AbuseIPDB - response was null. This is NOT a fatal error.");
            return false;
        }
        AntiScan.LOGGER.info("Updated IP blacklist from AbuseIPDB.");
        return true;
    }

    protected boolean fetchFromHunter() {
        HttpResponse<String> response;
        // https://curlconverter.com/java/
        //? if >=1.20.5
        try (HttpClient client = HttpClient.newHttpClient()) {
        //? if <1.20.5
        /*HttpClient client = HttpClient.newHttpClient();*/
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://raw.githubusercontent.com/pebblehost/hunter/refs/heads/master/ips.txt"))
                    .GET()
                    .build();

            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                AntiScan.LOGGER.warn("Failed to load ip blacklist from hunter. This is NOT a fatal error.", e);
                return false;
            }
        //? if >=1.20.5
        }
        if (response != null) {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                blacklistCache.addAll(Arrays.asList(response.body().split("\n")));
            } else {
                AntiScan.LOGGER.warn("Failed to load ip blacklist from hunter. This is NOT a fatal error. Status: {}. Body: {}", response.statusCode(), response.body());
                return false;
            }
        } else {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from hunter - response was null. This is NOT a fatal error.");
            return false;
        }
        AntiScan.LOGGER.info("Updated IP blacklist from Hunter.");
        return true;
    }

    public Set<String> getBlacklistCache() {
        return blacklistCache;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public Set<String> getManualBlacklist() {
        return manualBlacklist;
    }

    private Set<String> getWhitelistCache() {
        return whitelistCache;
    }

    public boolean isBlacklisted(String ip) {
        return blacklistCache.contains(ip) || manualBlacklist.contains(ip) || checkAbuseIpdb(ip);
    }

    public Future<Boolean> report(String ip, String comment, int[] categories) {
        FutureTask<Boolean> future = new FutureTask<>(() -> reportNow(ip, comment, categories));
        new Thread(future, "AntiScan Reporting").start();
        return future;
    }

    public boolean reportNow(String ip, String comment, int[] categories) {
        if (AntiScan.IS_DEV_MODE) {
            AntiScan.STATS.onIpReported(ip);
            return true;
        }
        if (AntiScan.CONFIG.getAbuseIpdbKey() != null &&
            !ip.equals("127.0.0.1") &&
            ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") &&
            System.currentTimeMillis() - reportingCache.getOrDefault(ip, 0L) > TimeUnit.MINUTES.toMillis(15)) {
            AntiScan.STATS.onIpReported(ip);
            reportingCache.put(ip, System.currentTimeMillis());
            HttpResponse<String> response;
            //? if >=1.20.5
            try (HttpClient client = HttpClient.newHttpClient()) {
            //? if <1.20.5
            /*HttpClient client = HttpClient.newHttpClient();*/
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.abuseipdb.com/api/v2/report"))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                String.format("ip=%s&categories=%s&comment=%s",
                                        ip,
                                        Arrays.stream(categories)
                                                .mapToObj(String::valueOf)
                                                .collect(Collectors.joining(",")),
                                        comment.replaceAll("\\W", "+"))))
                        .setHeader("Key", AntiScan.CONFIG.getAbuseIpdbKey())
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | InterruptedException e) {
                    AntiScan.LOGGER.warn("Failed to report IP. This is NOT a fatal error.", e);
                    return false;
                }
            //? if >=1.20.5
            }
            if (response != null) {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    if (AntiScan.CONFIG.shouldLogReports()) {
                        AntiScan.LOGGER.info("Reported {}.", ip);
                    }
                    return true;
                } else {
                    AntiScan.LOGGER.warn("Failed to report IP to AbuseIPDB. This is NOT a fatal error. Status: {}. Body: {}", response.statusCode(), response.body());
                    return false;
                }
            } else {
                AntiScan.LOGGER.warn("Failed to report IP to AbuseIPDB - response was null. This is NOT a fatal error.");
                return false;
            }
        }
        return false;
    }

    public void save(File file) throws IOException {
        Utils.saveToFile(this, file, CODEC);
    }

    public boolean unBlacklist(String ip, boolean manual, @Nullable File saveFile) throws IOException {
        boolean removed;
        if (manual) {
            removed = manualBlacklist.remove(ip);
        } else {
            removed = blacklistCache.remove(ip);
        }
        if (removed && saveFile != null) {
            save(saveFile);
        }
        return removed;
    }

    public Future<Boolean> update(long cooldown) {
        return update(cooldown, null);
    }

    public Future<Boolean> update(long cooldown, @Nullable File saveFile) {
        if (System.currentTimeMillis() - lastUpdated > cooldown) {
            return updateNow(saveFile);
        }
        return CompletableFuture.completedFuture(Boolean.FALSE);
    }

    public Future<Boolean> updateNow() {
        return updateNow(null);
    }

    /**
     * For testing and development only.
     *
     * @param success Whether the update should succeed
     * @param instant Whether the update should be instant or have some artificial dealy
     * @return {@code success}
     */
    @Contract("_,_,_->param1")
    private boolean simulateUpdate(boolean success, boolean instant, Random random) {
        if (!instant) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(random.nextInt(0, 4)));
            } catch (InterruptedException e) {
                AntiScan.LOGGER.debug("Interrupted while simulating blacklist update. It will still happen.", e);
            }
        }
        if (success) {
            for (int i = 0; i < random.nextInt(1, 6); i++) {
                blacklistCache.add(String.format("%d.%d.%d.%d",
                        random.nextInt(0, 256),
                        random.nextInt(0, 256),
                        random.nextInt(0, 256),
                        random.nextInt(0, 256)));
            }
        }
        return success;
    }

    public Future<Boolean> updateNow(@Nullable File saveFile) {
        if (fetchingUpdates.tryLock()) {
            try {
                lastUpdated = System.currentTimeMillis();
                AntiScan.LOGGER.info("Updating blacklisted IPs.");
                whitelistCache.clear();
                blacklistCache.clear();
                FutureTask<Boolean> hunter = new FutureTask<>(() -> {
                    if (AntiScan.IS_DEV_MODE) {
                        Random random = new Random();
                        return simulateUpdate(random.nextBoolean(), false, random);
                    }
                    return fetchFromHunter();
                });
                FutureTask<Boolean> abuseIpdb = new FutureTask<>(() -> {
                    if (AntiScan.IS_DEV_MODE) {
                        Random random = new Random();
                        return simulateUpdate(random.nextBoolean(), false, random);
                    }
                    if (AntiScan.CONFIG.getAbuseIpdbKey() != null) {
                        return fetchFromAbuseIpdb(AntiScan.CONFIG.getAbuseIpdbKey());
                    }
                    return false;
                });
                FutureTask<Boolean> finished = new FutureTask<>(() -> {
                    try {
                        boolean hunterSucceeded = hunter.get();
                        boolean abuseIpdbSucceeded = abuseIpdb.get();
                        if (saveFile != null) {
                            save(saveFile);
                        }
                        return hunterSucceeded || abuseIpdbSucceeded;
                    } catch (InterruptedException | ExecutionException e) {
                        AntiScan.LOGGER.warn("Failed to wait for Hunter and AbuseIPDB threads. This is NOT a fatal error.", e);
                        return false;
                    }
                });
                new Thread(hunter, "AntiScan Hunter").start();
                new Thread(abuseIpdb, "AntiScan AbuseIPDB").start();
                new Thread(finished, "AntiScan Save").start();
                return finished;
            } finally {
                fetchingUpdates.unlock();
            }
        }
        return CompletableFuture.completedFuture(Boolean.FALSE);
    }
}
