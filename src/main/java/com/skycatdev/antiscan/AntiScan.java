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
    public static final File IP_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_ips.json").toFile();
    public static final IpChecker IP_CHECKER = IpChecker.loadOrCreate(IP_CHECKER_FILE);
    public static final File NAME_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_names.json").toFile();
    public static final NameChecker NAME_CHECKER = NameChecker.loadOrCreate(NAME_CHECKER_FILE);

    static {
        if (!IP_CHECKER_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            IP_CHECKER_FILE.getParentFile().mkdirs();
        }
        if (!NAME_CHECKER_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            NAME_CHECKER_FILE.getParentFile().mkdirs();
        }
    }

    @Override
    public void onInitialize() {
        IP_CHECKER.update(TimeUnit.HOURS.toMillis(5), IP_CHECKER_FILE);
    }
}