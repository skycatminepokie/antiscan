package com.skycatdev.antiscan;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class IpChecker {
    public static final Codec<IpChecker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().xmap(list -> {
                Set<String> set = ConcurrentHashMap.newKeySet();
                set.addAll(list);
                return set;
            }, set -> set.stream().toList()).fieldOf("blacklist").forGetter(IpChecker::getBlacklistCache),
            Codec.LONG.fieldOf("lastUpdated").forGetter(IpChecker::getLastUpdated)
    ).apply(instance, IpChecker::new));

    protected long lastUpdated;
    protected final Set<String> blacklistCache;

    public Set<String> getBlacklistCache() {
        return blacklistCache;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    protected IpChecker(Set<String> blacklistCache, long lastUpdated) {
        this.blacklistCache = blacklistCache;
        this.lastUpdated = lastUpdated;
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
            return new IpChecker(new HashSet<>(), 0);
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from save file. This is NOT a detrimental error.", e);
            return new IpChecker(new HashSet<>(), 0);
        }
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

    public Future<Boolean> updateNow(String abuseIpdbKey) {
        FutureTask<Boolean> hunter = new FutureTask<>(this::fetchFromHunter);
        FutureTask<Boolean> abuseIpdb = new FutureTask<>(() -> fetchFromAbuseIpdb(abuseIpdbKey));
        FutureTask<Boolean> finished = new FutureTask<>(() -> {
            try {
                boolean hunterSucceeded = hunter.get();
                boolean abuseIpdbSucceeded = abuseIpdb.get();
                lastUpdated = System.currentTimeMillis();
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

    public Future<Boolean> update(String abuseIpdbKey, long cooldown) {
        if (System.currentTimeMillis() - lastUpdated > cooldown) {
            return updateNow(abuseIpdbKey);
        }
        return CompletableFuture.completedFuture(Boolean.FALSE);
    }

    protected void save(File file) throws IOException {
        assert file.exists();
        JsonElement json = CODEC.encode(this, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            Streams.write(json, writer);
        }
    }

    public boolean isBlacklisted(String ip) {
        return blacklistCache.contains(ip);
    }
}
