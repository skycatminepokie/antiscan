package com.skycatdev.antiscan;

import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

public class IpChecker {
    public static final Codec<IpChecker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().xmap(IpChecker::listToConcurrentSet, set -> set.stream().toList()).fieldOf("blacklistCache").forGetter(IpChecker::getBlacklistCache),
            Codec.LONG.fieldOf("lastUpdated").forGetter(IpChecker::getLastUpdated),
            Codec.STRING.optionalFieldOf("abuseIpdbKey").forGetter(checker -> Optional.ofNullable(checker.getAbuseIpdbKey())),
            Codec.STRING.listOf().xmap(IpChecker::listToConcurrentSet, set -> set.stream().toList()).fieldOf("manualBlacklist").forGetter(IpChecker::getManualBlacklist)
    ).apply(instance, (blacklistCache, lastUpdated, apiKey, manualBlacklist) -> new IpChecker(blacklistCache, lastUpdated, apiKey.orElse(null), manualBlacklist)));
    protected final Set<String> blacklistCache;
    protected final Set<String> manualBlacklist;
    protected long lastUpdated;
    protected @Nullable String abuseIpdbKey;

    protected IpChecker(Set<String> blacklistCache, long lastUpdated, @Nullable String abuseIpdbKey, Set<String> manualBlacklist) {
        this.blacklistCache = blacklistCache;
        this.lastUpdated = lastUpdated;
        this.abuseIpdbKey = abuseIpdbKey;
        this.manualBlacklist = manualBlacklist;
    }

    private static <T> @NotNull Set<T> listToConcurrentSet(List<T> list) {
        Set<T> set = ConcurrentHashMap.newKeySet();
        set.addAll(list);
        return set;
    }

    public static IpChecker load(File saveFile) throws IOException {
        AntiScan.LOGGER.info("Loading ip blacklist.");
        try (JsonReader reader = new JsonReader(new FileReader(saveFile))) {
            return CODEC.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow().getFirst();
        }
    }

    public static IpChecker loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            AntiScan.LOGGER.info("Creating a new ip blacklist.");
            return new IpChecker(ConcurrentHashMap.newKeySet(), 0, null, ConcurrentHashMap.newKeySet());
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from save file. This is NOT a detrimental error.", e);
            return new IpChecker(ConcurrentHashMap.newKeySet(), 0, null, ConcurrentHashMap.newKeySet());
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
     * @return True if the ip is deemed malicious (or on failure)
     */
    public boolean checkAbuseIpdb(String ip) {
        if (abuseIpdbKey == null) {
            return false;
        }
        AntiScan.LOGGER.info("Checking ip '{}'", ip);
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.abuseipdb.com/api/v2/check?ipAddress=%s", ip)))
                    .GET()
                    .setHeader("Key", abuseIpdbKey)
                    .setHeader("Accept", "application/json")
                    .timeout(Duration.of(5, TimeUnit.SECONDS.toChronoUnit()))
                    .build();
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB. This is NOT a fatal error.", e);
                return false;
            }
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
                    }
                    return safe;
                }
            } else {
                AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB. This is NOT a fatal error. Status: {}. Body: {}", response.statusCode(), response.body());
                return true;
            }
        } else {
            AntiScan.LOGGER.warn("Failed to load ip check from AbuseIPDB - response was null. This is NOT a fatal error.");
            return true;
        }
        return true;
    }

    // https://curlconverter.com/java/
    protected boolean fetchFromAbuseIpdb(String apiKey) {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
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
        return true;
    }

    protected boolean fetchFromHunter() {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
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
        return true;
    }

    // Yes I know protected does not make it hidden from other mods, or even hidden in memory.
    protected @Nullable String getAbuseIpdbKey() {
        return abuseIpdbKey;
    }

    public void setAbuseIpdbKey(@Nullable String abuseIpdbKey) {
        this.abuseIpdbKey = abuseIpdbKey;
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

    public boolean isBlacklisted(String ip) {
        return blacklistCache.contains(ip) || manualBlacklist.contains(ip) || checkAbuseIpdb(ip);
    }

    protected void save(File file) throws IOException {
        if (!file.exists()) {
            if (file.isDirectory() || !file.createNewFile()) {
                throw new FileNotFoundException();
            }
        }
        JsonElement json = CODEC.encode(this, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            Streams.write(json, writer);
        }
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

    public Future<Boolean> updateNow(@Nullable File saveFile) {
        lastUpdated = System.currentTimeMillis();
        AntiScan.LOGGER.info("Updating blacklisted IPs.");
        FutureTask<Boolean> hunter = new FutureTask<>(this::fetchFromHunter);
        FutureTask<Boolean> abuseIpdb = new FutureTask<>(() -> {
            if (abuseIpdbKey != null) {
                return fetchFromAbuseIpdb(abuseIpdbKey);
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
    }
}
