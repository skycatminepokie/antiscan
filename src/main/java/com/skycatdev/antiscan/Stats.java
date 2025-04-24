package com.skycatdev.antiscan;

import com.mojang.serialization.Codec;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class Stats {
    public static final Codec<Stats> CODEC = Codec.INT.xmap(Stats::new, Stats::getIpsReported);
    protected AtomicInteger ipsReported;

    public Stats(int ipsReported) {
        this.ipsReported = new AtomicInteger(ipsReported);
    }

    public static Stats load(File saveFile) throws IOException {
        return Utils.loadFromFile(saveFile, CODEC);
    }

    public static Stats loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            AntiScan.LOGGER.info("Creating a new stats file.");
            return new Stats(0);
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load stats from save file. This is NOT a detrimental error.", e);
            return new Stats(0);
        }
    }

    public int getIpsReported() {
        return ipsReported.get();
    }

    public void onIpReported(String ip) {
        ipsReported.getAndIncrement();
    }

    protected void save(File file) throws IOException {
        Utils.saveToFile(this, file, CODEC);
    }


}
