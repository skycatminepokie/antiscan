package com.skycatdev.antiscan;

import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;

public class Utils {
    public static Text textOf(String text) {
        return Text.literal(text);
    }

    public static Text translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }

    public static void handleIpConnection(Config.IpMode mode, Config.Action action, boolean report, ClientConnection connection, Runnable allow) {
        boolean good = switch (mode) {
            case MATCH_NONE -> true;
            case MATCH_ALL -> false;
            case MATCH_IP -> {
                if (connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                    yield connection.isLocal() || !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName());
                }
                yield connection.isLocal();
            }
        };

        if (good) {
            allow.run();
        } else {
            switch (action) {
                case NOTHING -> allow.run();
                case TARPIT -> {
                }
                case DISCONNECT -> connection.disconnect(Utils.translatable("multiplayer.disconnect.generic"));
                default -> {
                    AntiScan.LOGGER.error("Impossible case -action not handled. Allowing connection. Please report this at https://github.com/skycatminepokie/antiscan/issues.");
                    allow.run();
                }
            }
            if (report && connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                AntiScan.IP_CHECKER.report(inetSocketAddress.getHostString(), "Unsolicited connection attempt. Reported by AntiScan for Fabric.", new int[]{14});
            }
        }
    }
}
