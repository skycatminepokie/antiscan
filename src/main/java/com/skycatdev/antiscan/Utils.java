package com.skycatdev.antiscan;

//? if >=1.21.5

import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

public class Utils {
    public static void handleIpConnection(Config.IpMode mode, Config.Action action, boolean report, ClientConnection connection, Runnable allow) {
        @Nullable String hostString = null;
        if (connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
            hostString = inetSocketAddress.getHostString();
        }
        boolean good = switch (mode) {
            case MATCH_NONE -> true;
            case MATCH_ALL -> false;
            case MATCH_IP -> {
                if (hostString != null) {
                    yield connection.isLocal() || !AntiScan.IP_CHECKER.isBlacklisted(hostString);
                }
                yield connection.isLocal();
            }
        };

        if (good) {
            allow.run();
        } else {
            switch (action) {
                case NOTHING -> allow.run();
                case TARPIT -> AntiScan.LOGGER.info("Tarpitting {}.", hostString == null ? "connection" : hostString);
                case DISCONNECT -> {
                    AntiScan.LOGGER.info("Disconnecting {}.", hostString == null ? "connection" : hostString);
                    connection.disconnect(Utils.translatable("multiplayer.disconnect.generic"));
                }
                default -> {
                    AntiScan.LOGGER.error("Impossible case -action not handled. Allowing connection. Please report this at https://github.com/skycatminepokie/antiscan/issues.");
                    allow.run();
                }
            }
            if (report && hostString != null) {
                AntiScan.IP_CHECKER.report(hostString, "Bad connection attempt. Reported by AntiScan for Fabric.", new int[]{14});
            }
        }
    }

    public static <T> void saveToFile(T t, File file, Codec<T> codec) throws IOException {
        if (!file.exists()) {
            if (file.isDirectory() || !file.createNewFile()) {
                throw new FileNotFoundException();
            }
        }
        JsonElement json = codec.encode(t, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(IOException::new);
        try (JsonWriter writer = new JsonWriter(new PrintWriter(file))) {
            //? if >=1.21.5
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            //? if <1.21.5
            /*writer.setIndent("  ");*/
            Streams.write(json, writer);
        }
    }

    public static Text textOf(String text) {
        return Text.literal(text);
    }

    public static Text translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
}
