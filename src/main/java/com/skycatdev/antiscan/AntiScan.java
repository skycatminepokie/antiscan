package com.skycatdev.antiscan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AntiScan implements ModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean IS_DEV_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("antiscan.json").toFile();
    public static final Config CONFIG = Config.loadOrCreate(CONFIG_FILE);
    public static final File IP_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_ips.json").toFile();
    public static final IpChecker IP_CHECKER = IpChecker.loadOrCreate(IP_CHECKER_FILE);
    public static final File NAME_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_names.json").toFile();
    public static final NameChecker NAME_CHECKER = NameChecker.loadOrCreate(NAME_CHECKER_FILE);
    public static final long DEFAULT_BLACKLIST_UPDATE_COOLDOWN = IS_DEV_MODE ? TimeUnit.SECONDS.toMillis(30) : TimeUnit.HOURS.toMillis(5);
    public static final Timer BLACKLIST_UPDATER = new Timer("Antiscan Blacklist Updater", true);

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
        BLACKLIST_UPDATER.schedule(new TimerTask() {
            @Override
            public void run() {
                IP_CHECKER.update(DEFAULT_BLACKLIST_UPDATE_COOLDOWN, IP_CHECKER_FILE);
            }
        }, DEFAULT_BLACKLIST_UPDATE_COOLDOWN, DEFAULT_BLACKLIST_UPDATE_COOLDOWN + 10); // Extra time to please the update delay checker. Maybe a hack, but it doesn't need to be perfect.
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(new CommandHandler());
    }
}