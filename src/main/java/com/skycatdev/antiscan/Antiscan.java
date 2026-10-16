package com.skycatdev.antiscan;

import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.AntiscanCommands;
import com.skycatdev.antiscan.impl.Config;
import com.skycatdev.antiscan.impl.BackgroundTasks;
import com.skycatdev.antiscan.impl.ConnectionRates;
import com.skycatdev.antiscan.impl.checker.NetworkFeeds;
import com.skycatdev.antiscan.impl.checker.ConnectionCheckers;
import com.skycatdev.antiscan.impl.checker.LocalChecker;
import com.skycatdev.antiscan.impl.checker.MultiChecker;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

public class Antiscan implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @SuppressWarnings("unused") public static final String VERSION = /*$ mod_version*/ "2.0.0";
    @SuppressWarnings("unused") public static final String MINECRAFT = /*$ minecraft*/ "26.3";
    public static final Path SAVE_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("antiscan");
    public static final Config CONFIG;
    /** Active protection settings. Only tests replace this after startup. */
    public static volatile Config.Protection protection;
    public static final NetworkFeeds FEEDS = new NetworkFeeds(SAVE_DIRECTORY.resolve("feeds"));
    public static final ConnectionRates RATES = new ConnectionRates(SAVE_DIRECTORY.resolve("temporary-bans.json"));
    static {
        File saveDirFile = SAVE_DIRECTORY.toFile();
        if (!saveDirFile.exists()) {
            if (saveDirFile.mkdir()) {
                LOGGER.info("Created Antiscan save directory.");
            } else {
                LOGGER.error("Failed to create Antiscan save directory.");
                throw new RuntimeException("Failed to create Antiscan save directory.");
            }
        } else {
            if (!saveDirFile.isDirectory()) {
                LOGGER.error("Antiscan save directory is not a directory! Perhaps you have a file named 'antiscan' in your root folder - try deleting it if it's not important.");
                throw new RuntimeException("Antiscan save directory is not a directory!");
            }
        }
        CONFIG = Config.load();
        protection = CONFIG.protection();
        RATES.setLimits(protection.maxConnectionsPerMinute(), protection.maxStatusPerMinute(), protection.maxLoginsPerMinute());
    }

    /**
     * This checker runs first.
     */
    private static final ConnectionChecker CHECKER_BATCH_1 = new MultiChecker(List.of(
            LocalChecker.INSTANCE,
            CONFIG.ipWhitelist(),
            CONFIG.ipBlacklist(),
            CONFIG.nameWhitelist(),
            CONFIG.nameBlacklist(),
            CONFIG.hunterChecker()
    ));

    public static Identifier locate(String path) {
        //? if <1.21 {
        /*return new Identifier(MOD_ID, path);
        *///?} else
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeServer() {
        ConnectionCheckers.init();
        CommandRegistrationCallback.EVENT.register(AntiscanCommands::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            FEEDS.load();
            RATES.load(System.currentTimeMillis());
            BackgroundTasks.start(() -> {
                RATES.prune(System.currentTimeMillis());
                RATES.save();
                if (protection.networkFeeds()) FEEDS.refresh();
                CONFIG.hunterChecker().updateIfNeeded();
                CONFIG.abuseIpdbChecker().updateIfNeeded();
            });
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BackgroundTasks.stop();
            AntiscanCommands.stop();
            RATES.save();
            CONFIG.trySave();
        });
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> CONFIG.trySave());
    }

    /** Immediate local/cache decision; unknown remote reputation is allowed on this connection. */
    public static void handleConnection(Connection connection, @Nullable String name, Runnable allow) {
        handleConnection(connection, name, allow, ConnectionRates.Kind.CHECK);
    }

    public static void handleConnection(Connection connection, @Nullable String name, Runnable allow, ConnectionRates.Kind kind) {
        VerificationStatus status = CHECKER_BATCH_1.check(connection, name, Runnable::run).join();
        if (status == VerificationStatus.SUCCEED) { allow.run(); return; }
        boolean blocked = status == VerificationStatus.FAIL || status == VerificationStatus.FAIL_REPORT;
        Config.Protection settings = protection;
        if (connection.getRemoteAddress() instanceof InetSocketAddress socket && socket.getAddress() != null) {
            String ip = socket.getAddress().getHostAddress();
            if (status == VerificationStatus.FAIL_REPORT) CONFIG.abuseIpdbChecker().onFailReport(ip);
            if (settings.rateLimits() && RATES.blocked(ip, connection, kind, System.currentTimeMillis())) {
                blocked = true;
                if (RATES.needsSave()) BackgroundTasks.submit("save-bans", RATES::save);
            }
            if (settings.networkFeeds()) {
                if (!blocked) blocked = FEEDS.blocked(socket.getAddress());
                if (!blocked) FEEDS.observe(socket.getAddress());
            }
        }
        if (!blocked) blocked = CONFIG.abuseIpdbChecker().check(connection, name, Runnable::run).join() == VerificationStatus.FAIL;
        if (blocked) {
            onBlocked(connection, name);
            connection.disconnect(Component.translatable("multiplayer.disconnect.generic"));
        } else allow.run();
    }

    private static final java.util.concurrent.atomic.AtomicLong LAST_LOG = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicInteger SUPPRESSED = new java.util.concurrent.atomic.AtomicInteger();

    private static void onBlocked(Connection connection, @Nullable String name) {
        // At most one detailed line per second; the rest are counted so floods don't flood the log.
        long now = System.nanoTime(), last = LAST_LOG.get(); // nanoTime: immune to wall-clock jumps
        if (now - last < 1_000_000_000L && last != 0 || !LAST_LOG.compareAndSet(last, now)) {
            SUPPRESSED.incrementAndGet();
            LOGGER.debug("Blocking connection from {} (log sampled)", connection.getRemoteAddress());
            return;
        }
        int skipped = SUPPRESSED.getAndSet(0);
        if (skipped > 0) Antiscan.LOGGER.info("Blocked {} more connections since the last log line.", skipped);
        if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
            if (name == null) {
                Antiscan.LOGGER.info("Blocking connection from {}.", socketAddress.getAddress().getHostAddress());
            } else {
                Antiscan.LOGGER.info("Blocking connection from {} (attempted as {})", socketAddress.getAddress().getHostAddress(), name);
            }
        } else {
            Antiscan.LOGGER.info("Blocking connection from... somewhere? This probably shouldn't happen.");
        }
    }
}