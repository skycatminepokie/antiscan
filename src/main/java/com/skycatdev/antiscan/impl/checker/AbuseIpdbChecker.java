package com.skycatdev.antiscan.impl.checker;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.*;
import com.skycatdev.antiscan.impl.BackgroundTasks;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class AbuseIpdbChecker implements ConnectionChecker {
    public static final File KEY_FILE = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("antiscan/antiscan-key-do-not-share.txt").toFile();
    public static final long DEFAULT_UPDATE_DELAY = TimeUnit.HOURS.toSeconds(6);
    public static final long DEFAULT_LAST_UPDATED = 0L;
    public static final long DEFAULT_REPORT_COOLDOWN = TimeUnit.HOURS.toSeconds(24);
    public static final MapCodec<AbuseIpdbChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("blacklist").forGetter(AbuseIpdbChecker::exportBlacklist),
            Codec.LONG.optionalFieldOf("last_updated", DEFAULT_LAST_UPDATED).forGetter(AbuseIpdbChecker::getLastUpdated),
            Codec.LONG.optionalFieldOf("update_delay_seconds", DEFAULT_UPDATE_DELAY).forGetter(AbuseIpdbChecker::getUpdateDelay),
            Codec.LONG.optionalFieldOf("report_cooldown_seconds", DEFAULT_REPORT_COOLDOWN).forGetter(AbuseIpdbChecker::getReportCooldown),
            Codec.INT.optionalFieldOf("abuse_confidence_threshold", 90).forGetter(AbuseIpdbChecker::getAbuseConfidenceThreshold),
            Codec.BOOL.optionalFieldOf("auto_report", false).forGetter(AbuseIpdbChecker::isAutoReport)
    ).apply(instance, AbuseIpdbChecker::new));
    public static final int DEFAULT_ABUSE_CONFIDENCE_THRESHOLD = 90;
    protected @Nullable String key;
    private final long updateDelay, reportCooldown;
    private final int abuseConfidenceThreshold;
    /** Report fail_report matches to AbuseIPDB automatically. Off by default; /antiscan report always works. */
    private final boolean autoReport;
    private long lastFailureLog;
    private volatile long lastUpdated;
    private volatile Set<String> blacklist;
    private record Cached(boolean abusive, long expires) { }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> retryAfter = new ConcurrentHashMap<>();
    private final Map<String, Long> reportTimes = new HashMap<>();
    private long day, requests, reports, blacklistRequests, blockedUntil, nextRefresh;
    private Path ledger = KEY_FILE.toPath().resolveSibling("abuse-budget.json");
    private boolean ledgerLoaded, ledgerValid = true;
    private static final int CACHE_LIMIT = 8192, DAILY_CHECKS = 900, DAILY_REPORTS = 100;

    public AbuseIpdbChecker() { this(new HashSet<>(), 0, DEFAULT_UPDATE_DELAY, DEFAULT_REPORT_COOLDOWN, DEFAULT_ABUSE_CONFIDENCE_THRESHOLD); }
    public AbuseIpdbChecker(HashSet<String> list, long updated, long delay) { this(list, updated, delay, DEFAULT_REPORT_COOLDOWN); }
    public AbuseIpdbChecker(HashSet<String> list, long updated, long delay, long cooldown) { this(list, updated, delay, cooldown, DEFAULT_ABUSE_CONFIDENCE_THRESHOLD); }
    public AbuseIpdbChecker(List<String> list, long updated, long delay, long cooldown, int threshold) { this(new HashSet<>(list), updated, delay, cooldown, threshold, false); }
    public AbuseIpdbChecker(List<String> list, long updated, long delay, long cooldown, int threshold, boolean autoReport) { this(new HashSet<>(list), updated, delay, cooldown, threshold, autoReport); }
    public AbuseIpdbChecker(HashSet<String> list, long updated, long delay, long cooldown, int threshold) { this(list, updated, delay, cooldown, threshold, false); }
    public AbuseIpdbChecker(HashSet<String> list, long updated, long delay, long cooldown, int threshold, boolean autoReport) {
        blacklist = Set.copyOf(list);
        lastUpdated = updated;
        updateDelay = delay;
        reportCooldown = cooldown;
        abuseConfidenceThreshold = threshold;
        this.autoReport = autoReport;
        key = loadKey();
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

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String name, Executor ignored) {
        if (connection.getRemoteAddress() instanceof InetSocketAddress socket && socket.getAddress() != null) {
            String ip = socket.getAddress().getHostAddress();
            long now = System.currentTimeMillis();
            if (FeedLimits.fresh(lastUpdated, now) && blacklist.contains(ip)) return CompletableFuture.completedFuture(VerificationStatus.FAIL);
            Cached found = cache.get(ip);
            if (found != null && found.expires > now) return CompletableFuture.completedFuture(found.abusive ? VerificationStatus.FAIL : VerificationStatus.PASS);
            if (key != null && now >= retryAfter.getOrDefault(ip, 0L)) BackgroundTasks.submit("abuse:" + ip, () -> lookupNow(ip));
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    protected void lookupNow(String ip) {
        if (!reserve(false, null, false)) { retry(ip); return; } // cooldown stops re-queueing while budget/backoff blocks
        var result = Utils.sendHttpRequest(request("https://api.abuseipdb.com/api/v2/check?ipAddress=" + URLEncoder.encode(ip, StandardCharsets.UTF_8))
                .GET().build(), Utils.boundedBody());
        if (!result.isSuccess()) { failure(result); retry(ip); return; }
        try {
            var scoreElement = JsonParser.parseString(result.getOrThrow().body()).getAsJsonObject()
                    .getAsJsonObject("data").get("abuseConfidenceScore");
            if (!scoreElement.isJsonPrimitive() || !scoreElement.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid score");
            int score = Integer.parseInt(scoreElement.getAsString());
            if (score < 0 || score > 100) throw new IllegalArgumentException("Invalid score");
            synchronized (cache) {
                if (cache.size() >= CACHE_LIMIT) cache.keySet().stream().findFirst().ifPresent(cache::remove);
                cache.put(ip, new Cached(score > abuseConfidenceThreshold, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)));
            }
            retryAfter.remove(ip);
        } catch (RuntimeException e) { retry(ip); }
    }

    private void retry(String ip) {
        synchronized (retryAfter) {
            if (retryAfter.size() >= CACHE_LIMIT) retryAfter.keySet().stream().findFirst().ifPresent(retryAfter::remove);
            retryAfter.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        }
    }

    public boolean updateIfNeeded() {
        long now = System.currentTimeMillis();
        if (key == null || now < nextRefresh || now < lastUpdated + TimeUnit.SECONDS.toMillis(Math.max(60, updateDelay))) return true;
        nextRefresh = now + TimeUnit.MINUTES.toMillis(15);
        if (!reserve(false, null, true)) return false;
        var result = Utils.sendHttpRequest(request("https://api.abuseipdb.com/api/v2/blacklist?confidenceMinimum=" + abuseConfidenceThreshold)
                .setHeader("Accept", "text/plain").GET().build(), Utils.boundedBody());
        if (!result.isSuccess()) { failure(result); return false; }
        try {
            Set<String> replacement = FeedLimits.parseIps(result.getOrThrow().body());
            blacklist = replacement;
            lastUpdated = System.currentTimeMillis();
            Antiscan.LOGGER.info("Updated AbuseIPDB blacklist ({} IPs).", replacement.size());
            return true;
        } catch (RuntimeException e) {
            Antiscan.LOGGER.warn("Malformed AbuseIPDB feed; retaining last good data");
            return false;
        }
    }

    private HttpRequest.Builder request(String uri) {
        return HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(5))
                .setHeader("Key", Objects.requireNonNull(key)).setHeader("Accept", "application/json");
    }

    private void failure(com.mojang.serialization.DataResult<HttpResponse<String>> result) {
        long delay = TimeUnit.MINUTES.toMillis(5);
        var partial = result.resultOrPartial();
        long logNow = System.currentTimeMillis();
        if (logNow - lastFailureLog >= TimeUnit.MINUTES.toMillis(1)) { // don't repeat this for every queued lookup
            lastFailureLog = logNow;
            Antiscan.LOGGER.warn("AbuseIPDB request failed ({}); pausing requests for a while.",
                    partial.map(r -> "HTTP " + r.statusCode()).orElse("no response"));
        }
        if (partial.isPresent()) {
            HttpResponse<String> response = partial.get();
            if (response.statusCode() == 429) {
                delay = TimeUnit.HOURS.toMillis(1);
                try {
                    String header = response.headers().firstValue("Retry-After").orElse("");
                    long parsed;
                    try { parsed = TimeUnit.SECONDS.toMillis(Long.parseLong(header)); }
                    catch (NumberFormatException e) { parsed = java.time.ZonedDateTime.parse(header, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis(); }
                    delay = Math.max(delay, Math.min(TimeUnit.DAYS.toMillis(1), parsed));
                } catch (RuntimeException ignored) { }
            }
        }
        synchronized (this) {
            blockedUntil = Math.max(blockedUntil, System.currentTimeMillis() + delay);
            persistLedger();
        }
    }

    // Reservation is persisted before HTTP; a failed request still consumes its conservative budget.
    private synchronized boolean reserve(boolean report, @Nullable String ip, boolean bulk) {
        loadLedger();
        if (!ledgerValid || key == null) return false;
        long now = System.currentTimeMillis();
        if (now < blockedUntil) return false;
        long today = now / TimeUnit.DAYS.toMillis(1);
        if (day != today) { day = today; requests = 0; reports = 0; blacklistRequests = 0; }
        reportTimes.values().removeIf(time -> now - time >= TimeUnit.SECONDS.toMillis(Math.max(0, reportCooldown)));
        if (report) {
            if (reportTimes.containsKey(ip) || reports >= DAILY_REPORTS || reportTimes.size() >= CACHE_LIMIT) return false;
            reportTimes.put(ip, now);
            reports++;
        } else {
            if (requests >= DAILY_CHECKS || (bulk && blacklistRequests >= 5)) return false;
            requests++;
            if (bulk) blacklistRequests++;
        }
        return persistLedger();
    }

    private void loadLedger() {
        if (ledgerLoaded) return;
        ledgerLoaded = true;
        if (!Files.exists(ledger)) return;
        try {
            if (Files.size(ledger) > 1024 * 1024) throw new IOException("Oversized ledger");
            JsonObject json = JsonParser.parseString(Files.readString(ledger)).getAsJsonObject();
            day = json.get("day").getAsLong(); requests = json.get("requests").getAsLong(); reports = json.get("reports").getAsLong();
            blockedUntil = json.get("blockedUntil").getAsLong();
            blacklistRequests = json.has("blacklistRequests") ? json.get("blacklistRequests").getAsLong() : 5;
            if (day < 0 || requests < 0 || reports < 0 || blacklistRequests < 0 || blockedUntil < 0 || json.getAsJsonObject("reportTimes").size() > CACHE_LIMIT)
                throw new IOException("Invalid budget state");
            for (var entry : json.getAsJsonObject("reportTimes").entrySet()) reportTimes.put(entry.getKey(), entry.getValue().getAsLong());
        } catch (IOException | RuntimeException e) {
            ledgerValid = false;
            Antiscan.LOGGER.warn("AbuseIPDB ledger unreadable; remote requests disabled to avoid resetting quotas", e);
        }
    }

    private boolean persistLedger() {
        JsonObject json = new JsonObject();
        json.addProperty("day", day); json.addProperty("requests", requests); json.addProperty("reports", reports); json.addProperty("blockedUntil", blockedUntil);
        json.addProperty("blacklistRequests", blacklistRequests);
        JsonObject times = new JsonObject(); reportTimes.forEach(times::addProperty); json.add("reportTimes", times);
        try { Utils.atomicWrite(ledger, json.toString()); return true; }
        catch (IOException e) { Antiscan.LOGGER.warn("Unable to save AbuseIPDB budget; request suppressed", e); return false; }
    }

    void useLedger(Path file) { ledger = file; }
    public CompletableFuture<Boolean> report(String ip, Executor executor) { return CompletableFuture.supplyAsync(() -> reportNow(ip), executor); }
    public CompletableFuture<Boolean> report(Connection connection, Executor executor) { return CompletableFuture.supplyAsync(() -> reportNow(connection), executor); }
    /** Called when a fail_report entry matched. Reports in the background, but only if the owner turned auto_report on. */
    public void onFailReport(String ip) {
        if (autoReport) BackgroundTasks.submit("report:" + ip, () -> reportNow(ip));
    }
    public boolean reportNow(Connection connection) {
        return connection.getRemoteAddress() instanceof InetSocketAddress socket && socket.getAddress() != null && reportNow(socket.getAddress().getHostAddress());
    }
    public boolean reportNow(String ip) {
        try { ip = InetAddresses.forString(ip).getHostAddress(); }
        catch (IllegalArgumentException e) { return false; }
        if (!reserve(true, ip, false)) return false;
        var result = Utils.sendHttpRequest(request("https://api.abuseipdb.com/api/v2/report")
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8)
                        + "&categories=14&comment=Port%20scan%20to%20Minecraft%20server."))
                .build(), Utils.boundedBody());
        if (!result.isSuccess()) failure(result);
        return result.isSuccess();
    }
    private List<String> exportBlacklist() { return List.copyOf(blacklist); }
    private long getLastUpdated() { return lastUpdated; }
    private long getUpdateDelay() { return updateDelay; }
    public long getReportCooldown() { return reportCooldown; }
    public int getAbuseConfidenceThreshold() { return abuseConfidenceThreshold; }
    public boolean isAutoReport() { return autoReport; }
    @Override public ConnectionCheckerType<?> getType() { return ConnectionCheckers.ABUSE_IPDB; }
}
