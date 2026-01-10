package com.skycatdev.antiscan;

import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.AntiscanCommands;
import com.skycatdev.antiscan.impl.Config;
import com.skycatdev.antiscan.impl.checker.ConnectionCheckers;
import com.skycatdev.antiscan.impl.checker.LocalChecker;
import com.skycatdev.antiscan.impl.checker.MultiChecker;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Antiscan implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antiscan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "2.0.0";
    public static final String MINECRAFT = /*$ minecraft*/ "1.21.11";
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
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CONFIG.trySave();
        });
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