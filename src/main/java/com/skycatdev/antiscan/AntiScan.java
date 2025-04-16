package com.skycatdev.antiscan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AntiScan implements ModInitializer, ServerTickEvents.StartTick {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("antiscan.json").toFile();
    public static final Config CONFIG = Config.loadOrCreate(CONFIG_FILE);
    public static final File IP_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_ips.json").toFile();
    public static final IpChecker IP_CHECKER = IpChecker.loadOrCreate(IP_CHECKER_FILE);
    public static final File NAME_CHECKER_FILE = FabricLoader.getInstance().getGameDir().resolve("data").resolve("antiscan_names.json").toFile();
    public static final NameChecker NAME_CHECKER = NameChecker.loadOrCreate(NAME_CHECKER_FILE);
    public static final long DEFAULT_BLACKLIST_UPDATE_COOLDOWN = TimeUnit.HOURS.toMillis(5);
    protected long updateAt = DEFAULT_BLACKLIST_UPDATE_COOLDOWN + System.currentTimeMillis();

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
    }

    @Override
    public void onInitialize() {
        IP_CHECKER.update(DEFAULT_BLACKLIST_UPDATE_COOLDOWN, IP_CHECKER_FILE);
        CommandRegistrationCallback.EVENT.register(new CommandHandler());
        ServerTickEvents.START_SERVER_TICK.register(this);
    }

    @Override
    public void onStartTick(MinecraftServer minecraftServer) {
        if (updateAt < System.currentTimeMillis()) {
            IP_CHECKER.update(DEFAULT_BLACKLIST_UPDATE_COOLDOWN, IP_CHECKER_FILE);
            updateAt = DEFAULT_BLACKLIST_UPDATE_COOLDOWN + System.currentTimeMillis();
        }
    }
}