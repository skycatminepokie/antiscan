package com.skycatdev.antiscan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
        Antiscan.LOGGER.info("Loading username blacklist.");
        return Utils.loadFromFile(saveFile, CODEC);
    }

    public static NameChecker loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            Antiscan.LOGGER.info("Creating a new name blacklist.");
            return new NameChecker(ConcurrentHashMap.newKeySet());
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            Antiscan.LOGGER.warn("Failed to load name blacklist from save file. This is NOT a detrimental error.", e);
            return new NameChecker(ConcurrentHashMap.newKeySet());
        }
    }

    public boolean blacklist(String name) {
        try {
            return blacklist(name, null);
        } catch (IOException e) {
            Antiscan.LOGGER.warn("Failed to save name blacklist. This is NOT a fatal error.", e);
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
        Utils.saveToFile(this, file, CODEC);
    }

    public boolean unBlacklist(String name) {
        try {
            return unBlacklist(name, null);
        } catch (IOException e) {
            Antiscan.LOGGER.warn("Failed to save name blacklist, even though we weren't trying?", e);
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
