package com.skycatdev.antiscan;

import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.Config;
import com.skycatdev.antiscan.impl.ConnectionCheckers;
import com.skycatdev.antiscan.impl.LocalChecker;
import com.skycatdev.antiscan.impl.MultiChecker;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

// TODO: filtering annoying log spam from scanners
public class Antiscan implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "0.0.1";// TODO: bump to 2.0.0
    public static final String MINECRAFT = /*$ minecraft*/ "1.21.7";
    public static final Config CONFIG = Config.load();
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

    public static ResourceLocation locate(String path) {
        //? if <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
        *///?} else
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeServer() {
        ConnectionCheckers.init();
    }

    /**
     * Warning: blocking
     */
    public static void handleConnection(Connection connection, @Nullable String name, Runnable allow) {
        try {
            VerificationStatus status = CHECKER_BATCH_1.check(connection, name, Runnable::run).get();
            switch (status) {
                case SUCCEED -> allow.run();
                case FAIL ->  // abuseIpdbChecker is not allowed to succeed. Therefore, FAIL is the end result.
                        connection.disconnect(Component.translatable("multiplayer.disconnect.generic"));
                case FAIL_REPORT -> { // abuseIpdbChecker is not allowed to succeed. Therefore, FAIL_REPORT is the end result.
                    connection.disconnect(Component.translatable("multiplayer.disconnect.generic"));
                    CONFIG.abuseIpdbChecker().reportNow(connection);
                }
                case PASS -> {
                    status = CONFIG.abuseIpdbChecker().check(connection, name, Runnable::run).get();
                    if (status == VerificationStatus.FAIL) {
                        allow.run();
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Failed to check connection.", e);
            allow.run();
        }
    }
}