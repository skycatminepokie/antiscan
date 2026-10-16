package com.skycatdev.antiscan.impl;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skycatdev.antiscan.Antiscan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ConnectionRates {
    public enum Kind { STATUS, LOGIN, CHECK }
    public record Ban(long expires, String reason) { }
    public static final int MAX_IPS = 16_384;
    public static final long WINDOW = TimeUnit.MINUTES.toMillis(1), BAN_TIME = TimeUnit.MINUTES.toMillis(10);
    private record Window(long start, int connections, int status, int login) { }
    private final Map<String, Window> windows = new LinkedHashMap<>();
    private final Map<String, Ban> bans = new HashMap<>();
    private final Map<Object, Boolean> seen = new WeakHashMap<>();
    private final Path file;
    private volatile int maxConnections = 240, maxStatus = 180, maxLogins = 60;
    private volatile long revision, savedRevision;
    public boolean needsSave() { return savedRevision != revision; }

    public ConnectionRates(Path file) { this.file = file; }

    public void setLimits(int connections, int status, int logins) {
        maxConnections = connections;
        maxStatus = status;
        maxLogins = logins;
    }

    public synchronized boolean blocked(String ip, Object connection, Kind kind, long now) {
        Ban ban = bans.get(ip);
        if (ban != null) {
            if (ban.expires > now) return true;
            bans.remove(ip);
            revision++;
        }
        if (kind == Kind.CHECK || seen.containsKey(connection)) return false;
        if (seen.size() >= MAX_IPS) seen.clear();
        seen.put(connection, true);
        Window old = windows.get(ip);
        if (old == null || now - old.start >= WINDOW) old = new Window(now, 0, 0, 0);
        Window next = new Window(old.start, old.connections + 1,
                old.status + (kind == Kind.STATUS ? 1 : 0), old.login + (kind == Kind.LOGIN ? 1 : 0));
        if (!windows.containsKey(ip) && windows.size() >= MAX_IPS) windows.remove(windows.keySet().iterator().next());
        windows.put(ip, next);
        String reason = next.connections > maxConnections ? "connection rate" : next.status > maxStatus ? "status rate" : next.login > maxLogins ? "login rate" : null;
        if (reason == null) return false;
        if (bans.size() >= MAX_IPS) prune(now);
        if (bans.size() < MAX_IPS) {
            bans.put(ip, new Ban(now + BAN_TIME, reason));
            revision++;
        }
        return true;
    }

    public synchronized void prune(long now) {
        if (bans.values().removeIf(b -> b.expires <= now)) revision++;
        windows.values().removeIf(w -> now - w.start >= WINDOW);
    }

    public synchronized Map<String, Ban> list(long now) { prune(now); return Map.copyOf(bans); }

    public synchronized boolean unban(String ip) {
        windows.remove(ip);
        boolean removed = bans.remove(ip) != null;
        if (removed) revision++;
        return removed;
    }

    public void load(long now) {
        try {
            if (!Files.exists(file)) return;
            if (Files.size(file) > 4 * 1024 * 1024) throw new IOException("Oversized ban state");
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            Map<String, Ban> loaded = new HashMap<>();
            for (var entry : json.entrySet()) {
                String ip = InetAddresses.forString(entry.getKey()).getHostAddress();
                JsonObject value = entry.getValue().getAsJsonObject();
                long expires = value.get("expires").getAsLong();
                if (expires > now && expires <= now + BAN_TIME) loaded.put(ip, new Ban(expires, value.get("reason").getAsString()));
                if (loaded.size() > MAX_IPS) throw new IOException("Too many bans");
            }
            synchronized (this) { bans.putAll(loaded); }
        } catch (IOException | RuntimeException e) { Antiscan.LOGGER.warn("Could not load temporary bans", e); }
    }

    private final Object saveLock = new Object();

    public void save() {
        synchronized (saveLock) {
            JsonObject json = new JsonObject();
            long snapshotRevision;
            synchronized (this) {
                if (savedRevision == revision) return;
                snapshotRevision = revision;
                bans.forEach((ip, ban) -> {
                    JsonObject value = new JsonObject();
                    value.addProperty("expires", ban.expires);
                    value.addProperty("reason", ban.reason);
                    json.add(ip, value);
                });
            }
            try {
                Utils.atomicWrite(file, json.toString());
                synchronized (this) { savedRevision = snapshotRevision; }
            } catch (IOException e) { Antiscan.LOGGER.warn("Could not persist temporary bans", e); }
        }
    }
}
