package com.skycatdev.antiscan;

import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NameChecker {
    public static Codec<NameChecker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().xmap(list -> {
                Set<String> set = ConcurrentHashMap.newKeySet();
                set.addAll(list);
                return set;
            }, set -> set.stream().toList()).fieldOf("blacklist").forGetter(NameChecker::getBlacklistCache)
    ).apply(instance, NameChecker::new));

    protected final Set<String> blacklistCache;

    protected NameChecker(Set<String> blacklistCache) {
        this.blacklistCache = blacklistCache;
    }

    public static NameChecker load(File saveFile) throws IOException {
        AntiScan.LOGGER.info("Loading username blacklist.");
        try (JsonReader reader = new JsonReader(new FileReader(saveFile))) {
            return CODEC.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow().getFirst();
        }
    }

    public static NameChecker loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            AntiScan.LOGGER.info("Creating a new name blacklist.");
            return new NameChecker(ConcurrentHashMap.newKeySet());
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load name blacklist from save file. This is NOT a detrimental error.", e);
            return new NameChecker(ConcurrentHashMap.newKeySet());
        }
    }

    public void blacklist(String name) {
        blacklistCache.add(name);
    }

    public Set<String> getBlacklistCache() {
        return blacklistCache;
    }

    public boolean isBlacklisted(String name) {
        return blacklistCache.contains(name);
    }
}
