package com.skycatdev.antiscan;

//? if >=1.21.5

import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetSocketAddress;

public class Utils {
    /**
     *
     * @return {@code true} if the connection was allowed/good, {@code false} if it was detected as a scanner
     */
    public static boolean handleNameIpConnection(Connection connection, String name, Config.NameIpMode mode, Config.Action action, boolean report, Runnable allow) {
        boolean good = switch (mode) {
            case MATCH_EITHER ->
                    !(!Antiscan.CONNECTION_CHECKER.shouldAllow(connection.getRemoteAddress()) || Antiscan.NAME_CHECKER.isBlacklisted(name));
            case MATCH_NONE -> true;
            case MATCH_ALL -> false;
            case MATCH_BOTH ->
                    !(!Antiscan.CONNECTION_CHECKER.shouldAllow(connection.getRemoteAddress()) && Antiscan.NAME_CHECKER.isBlacklisted(name));
            case MATCH_IP -> Antiscan.CONNECTION_CHECKER.shouldAllow(connection.getRemoteAddress());
            case MATCH_NAME -> !Antiscan.NAME_CHECKER.isBlacklisted(name);
        };
        if (good) {
            allow.run();
            return true;
        } else {
            @Nullable String hostString = null;
            if (connection.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                hostString = inetSocketAddress.getHostString();
            }
            return handleAction(action, report, connection, allow, hostString);
        }
    }

    /**
     *
     * @return {@code true} if the connection was allowed/good, {@code false} if it was detected as a scanner
     */
    public static boolean handleIpConnection(Connection connection, Config.IpMode mode, Config.Action action, boolean report, Runnable allow) {
        boolean good = switch (mode) {
            case MATCH_NONE -> true;
            case MATCH_ALL -> false;
            case MATCH_IP -> Antiscan.CONNECTION_CHECKER.shouldAllow(connection.getRemoteAddress());
        };
        if (good) {
            allow.run();
            return true;
        } else {
            @Nullable String hostString = null;
            if (connection.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                hostString = inetSocketAddress.getHostString();
            }
            return handleAction(action, report, connection, allow, hostString);
        }
    }

    private static boolean handleAction(Config.Action action, boolean report, Connection connection, Runnable allow, @Nullable String hostString) {
        switch (action) {
            case NOTHING -> allow.run();
            case TIMEOUT -> {
                if (Antiscan.CONFIG.shouldLogActions()) {
                    Antiscan.LOGGER.info("Timing out {}.", hostString == null ? "connection" : hostString);
                }
            }
            case DISCONNECT -> {
                if (Antiscan.CONFIG.shouldLogActions()) {
                    Antiscan.LOGGER.info("Disconnecting {}.", hostString == null ? "connection" : hostString);
                }
                connection.disconnect(Utils.translatable("multiplayer.disconnect.generic"));
            }
            default -> {
                Antiscan.LOGGER.error("Impossible case - action not handled. Allowing connection. Please report this at https://github.com/skycatminepokie/antiscan/issues.");
                allow.run();
                return true;
            }
        }
        if (report && hostString != null) {
            Antiscan.CONNECTION_CHECKER.report(hostString, "Bad connection attempt. Reported by Antiscan for Fabric.", new int[]{14});
        }
        return false;
    }

    public static <T> void saveToFile(T t, File file, Codec<T> codec) throws IOException {
        if (!file.exists()) {
            if (file.isDirectory() || !file.createNewFile()) {
                throw new FileNotFoundException();
            }
        }
        JsonElement json;
        //? if >=1.20.5
        json = codec.encode(t, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        //? if <1.20.5 {
        /*try {
            json = codec.encode(t, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(false, str -> {});
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
        *///?}
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            //? if >=1.21.5
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            //? if <1.21.5
            /*writer.setIndent("  ");*/
            Streams.write(json, writer);
        }
    }

    public static <T> T loadFromFile(File file, Codec<T> codec) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            //? if >=1.20.5
            return codec.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow().getFirst();
            //? if <1.20.5 {
            /*try {
                return codec.decode(JsonOps.INSTANCE, Streams.parse(reader)).getOrThrow(false, str -> {}).getFirst();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            *///?}
        }
    }

    public static Component textOf(String text) {
        return Component.literal(text);
    }

    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
