package com.skycatdev.antiscan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AntiScan implements ModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File IP_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("antiscan-ips.txt").toFile();
    public static final IpChecker IP_CHECKER = IpChecker.loadOrCreate(IP_CHECKER_FILE);

    @Override
    public void onInitialize() {
        IP_CHECKER.update(TimeUnit.HOURS.toMillis(5), IP_CHECKER_FILE);
    }
}