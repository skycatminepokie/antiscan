package com.skycatdev.antiscan;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NameChecker {
    public static Codec<NameChecker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().xmap(list -> {
                Set<String> set = ConcurrentHashMap.newKeySet();
                set.addAll(list);
                return set;
            }, set -> set.stream().toList()).fieldOf("blacklist").forGetter(NameChecker::getBlacklist)
    ).apply(instance, NameChecker::new));

    protected final Set<String> blacklist;

    protected NameChecker(Set<String> blacklist) {
        this.blacklist = blacklist;
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

    public boolean blacklist(String name) {
        try {
            return blacklist(name, null);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to save name blacklist. This is NOT a fatal error.", e);
            return false;
        }
    }

    /**
     *
     * @return {@code false} iff saving was attempted and failed.
     */
    public boolean blacklist(String name, @Nullable File saveFile) throws IOException {
        boolean added = blacklist.add(name);
        if (added && saveFile != null) {
            save(saveFile);
        }
        return added;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public boolean isBlacklisted(String name) {
        return blacklist.contains(name);
    }

    protected void save(File file) throws IOException {
        if (!file.exists()) {
            if (file.isDirectory() || !file.createNewFile()) {
                throw new FileNotFoundException();
            }
        }
        JsonElement json = CODEC.encode(this, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            Streams.write(json, writer);
        }
    }

    public boolean unBlacklist(String name) {
        try {
            return unBlacklist(name, null);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to save name blacklist, even though we weren't trying?", e);
            return false;
        }
    }

    public boolean unBlacklist(String name, @Nullable File saveFile) throws IOException {
        boolean removed = blacklist.remove(name);
        if (removed && saveFile != null) {
            save(saveFile);
        }
        return removed;
    }
}
