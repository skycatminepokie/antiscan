package com.skycatdev.antiscan.impl.checker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.impl.Cidr;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Last-good immutable snapshots; raw DROP records retain upstream attribution and terms. */
public final class NetworkFeeds {
    private final Path directory;
    private final Map<String, Snapshot> snapshots = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> nextRefresh = new HashMap<>();
    private final Map<String, Long> observations = new LinkedHashMap<>();
    private record Snapshot(List<Cidr> ranges, Set<String> ips, long fetched) { }

    public NetworkFeeds(Path directory) { this.directory = directory; }

    private static final Map<String, String> URLS = Map.of(
            "drop-v4", "https://www.spamhaus.org/drop/drop_v4.json",
            "drop-v6", "https://www.spamhaus.org/drop/drop_v6.json",
            "ipsum", "https://raw.githubusercontent.com/stamparm/ipsum/master/levels/3.txt");

    public void load() {
        for (String source : URLS.keySet()) {
            Path file = directory.resolve(source + ".json");
            try {
                if (!Files.exists(file) || Files.size(file) > FeedLimits.MAX_FEED_BYTES * 2L + 1024) continue;
                JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                install(source, saved.get("body").getAsString(), saved.get("fetched").getAsLong());
            } catch (IOException | RuntimeException e) { Antiscan.LOGGER.warn("Ignoring invalid saved feed {}", source); }
        }
    }

    public void refresh() {
        for (var source : URLS.entrySet()) {
            long now = System.currentTimeMillis();
            if (now < nextRefresh.getOrDefault(source.getKey(), 0L)) continue;
            nextRefresh.put(source.getKey(), now + TimeUnit.MINUTES.toMillis(15));
            try {
                var result = Utils.sendHttpRequest(HttpRequest.newBuilder(URI.create(source.getValue()))
                        .timeout(Duration.ofSeconds(10)).GET().build(), Utils.boundedBody());
                if (!result.isSuccess()) {
                    Antiscan.LOGGER.warn("Feed {} download failed ({}); keeping last good snapshot", source.getKey(),
                            result.resultOrPartial().map(r -> "HTTP " + r.statusCode()).orElse("no response"));
                    continue;
                }
                String body = result.getOrThrow().body();
                Snapshot parsed = parse(source.getKey(), body, now);
                JsonObject saved = new JsonObject();
                saved.addProperty("fetched", now);
                saved.addProperty("source", source.getValue());
                saved.addProperty("body", body);
                Utils.atomicWrite(directory.resolve(source.getKey() + ".json"), saved.toString());
                snapshots.put(source.getKey(), parsed);
                nextRefresh.put(source.getKey(), now + TimeUnit.HOURS.toMillis(3));
                Antiscan.LOGGER.info("Updated feed {}", source.getKey());
            } catch (IOException | RuntimeException e) { Antiscan.LOGGER.warn("Feed {} refresh failed; keeping last good snapshot", source.getKey()); }
        }
    }

    void install(String source, String body, long fetched) { snapshots.put(source, parse(source, body, fetched)); }

    private static Snapshot parse(String source, String body, long fetched) {
        if (body.length() > FeedLimits.MAX_FEED_BYTES || body.isBlank()) throw new IllegalArgumentException("Invalid feed size");
        if (source.equals("ipsum")) return new Snapshot(List.of(), FeedLimits.parseIps(body), fetched);
        List<Cidr> ranges = new ArrayList<>();
        boolean metadata = false;
        int recordCount = -1;
        for (var lines = body.lines().iterator(); lines.hasNext();) {
            String line = lines.next();
            if (line.isBlank()) continue;
            JsonObject record = JsonParser.parseString(line).getAsJsonObject();
            if (record.has("cidr")) {
                if (metadata) throw new IllegalArgumentException("Records after metadata");
                Cidr cidr = Cidr.parse(record.get("cidr").getAsString());
                if (cidr.network().length != (source.equals("drop-v4") ? 4 : 16)) throw new IllegalArgumentException("Wrong address family");
                ranges.add(cidr);
                if (ranges.size() > FeedLimits.MAX_FEED_ENTRIES) throw new IllegalArgumentException("Too many ranges");
            } else {
                if (metadata || !record.has("copyright") || !record.has("terms") || !record.has("timestamp")
                        || !record.has("records") || !record.has("type") || !record.get("type").getAsString().equals("metadata")
                        || record.get("copyright").getAsString().isBlank() || record.get("terms").getAsString().isBlank())
                    throw new IllegalArgumentException("Missing DROP metadata");
                recordCount = record.get("records").getAsInt();
                long timestamp = record.get("timestamp").getAsLong();
                if (timestamp <= 0 || timestamp > Long.MAX_VALUE / 1000) throw new IllegalArgumentException("Invalid DROP date");
                fetched = Math.min(fetched, timestamp * 1000);
                metadata = true;
            }
        }
        if (!metadata || ranges.isEmpty() || recordCount != ranges.size()) throw new IllegalArgumentException("Incomplete DROP feed");
        return new Snapshot(List.copyOf(ranges), Set.of(), fetched);
    }

    public boolean blocked(InetAddress ip) {
        long now = System.currentTimeMillis();
        byte[] address = ip.getAddress();
        Snapshot snapshot = snapshots.get(address.length == 4 ? "drop-v4" : "drop-v6");
        return snapshot != null && FeedLimits.fresh(snapshot.fetched, now) && containsAny(snapshot.ranges, address);
    }

    private static boolean containsAny(List<Cidr> ranges, byte[] address) {
        for (int i = 0; i < ranges.size(); i++) if (ranges.get(i).contains(address)) return true;
        return false;
    }

    public void observe(InetAddress ip) {
        Snapshot snapshot = snapshots.get("ipsum");
        long now = System.currentTimeMillis();
        String key = ip.getHostAddress();
        if (snapshot == null || !FeedLimits.fresh(snapshot.fetched, now) || !snapshot.ips.contains(key)) return; // lock-free miss path
        observeHit(key, now);
    }

    private synchronized void observeHit(String key, long now) {
        if (now < observations.getOrDefault(key, 0L)) return;
        if (observations.size() >= 4096) observations.remove(observations.keySet().iterator().next());
        observations.put(key, now + TimeUnit.HOURS.toMillis(1));
        Antiscan.LOGGER.info("IPsum observation for {} (not a blocking signal)", key);
    }

}
