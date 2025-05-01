package com.skycatdev.antiscan;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AntiScan implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean IS_DEV_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("antiscan.json").toFile();
    public static final Config CONFIG = Config.loadOrCreate(CONFIG_FILE);
    public static final File IP_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_ips.json").toFile();
    public static final IpChecker IP_CHECKER = IpChecker.loadOrCreate(IP_CHECKER_FILE);
    public static final File NAME_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_names.json").toFile();
    public static final NameChecker NAME_CHECKER = NameChecker.loadOrCreate(NAME_CHECKER_FILE);
    public static final Timer BLACKLIST_UPDATER = new Timer("Antiscan Blacklist Updater", true);
    public static final File STATS_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_stats.json").toFile();
    public static final Stats STATS = Stats.loadOrCreate(STATS_FILE);

    static {
        if (!IP_CHECKER_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            IP_CHECKER_FILE.getParentFile().mkdirs();
        }
        if (!NAME_CHECKER_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            NAME_CHECKER_FILE.getParentFile().mkdirs();
        }
        if (!CONFIG_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            CONFIG_FILE.getParentFile().mkdirs();
        }
        if (!STATS_FILE.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            STATS_FILE.getParentFile().mkdirs();
        }

        BLACKLIST_UPDATER.schedule(new TimerTask() {
            @Override
            public void run() {
                IP_CHECKER.update(CONFIG.getBlacklistUpdateCooldown(), IP_CHECKER_FILE);
            }
        }, CONFIG.getBlacklistUpdateCooldown(), CONFIG.getBlacklistUpdateCooldown() + 10); // Extra time to please the update delay checker. Maybe a hack, but it doesn't need to be perfect.
    }

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register(new CommandHandler());
        //? if >=1.21.5
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
            //? if <1.21.5
            /*ServerLifecycleEvents.SERVER_STOPPING.register(server -> {*/
            try {
                STATS.save(STATS_FILE);
            } catch (IOException e) {
                LOGGER.warn("Failed to save Antiscan stats. This is NOT a fatal error.");
            }
        });
    }
}