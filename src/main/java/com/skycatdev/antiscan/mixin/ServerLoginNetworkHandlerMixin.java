package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import com.skycatdev.antiscan.Utils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.net.InetSocketAddress;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    ClientConnection connection;

    @WrapMethod(method = "onHello")
    private void antiScan$tarpitBaddies(LoginHelloC2SPacket packet, Operation<Void> original) {
        boolean good = switch (AntiScan.CONFIG.getLoginMode()) {
            case MATCH_EITHER -> {
                if (connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                    yield connection.isLocal() ||
                          !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName()) ||
                          !AntiScan.NAME_CHECKER.isBlacklisted(packet.name());
                }
                yield connection.isLocal() || !AntiScan.NAME_CHECKER.isBlacklisted(packet.name());
            }
            case MATCH_NONE -> true;
            case MATCH_ALL -> false;
            case MATCH_BOTH -> {
                if (connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                    yield connection.isLocal() ||
                          !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName()) && !AntiScan.NAME_CHECKER.isBlacklisted(packet.name());
                }
                yield connection.isLocal() && !AntiScan.NAME_CHECKER.isBlacklisted(packet.name());
            }
            case MATCH_IP -> {
                if (connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                    yield connection.isLocal() || !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName());
                }
                yield connection.isLocal();
            }
            case MATCH_NAME -> !AntiScan.NAME_CHECKER.isBlacklisted(packet.name());
        };

        if (good) {
            original.call(packet);
        } else {
            switch (AntiScan.CONFIG.getLoginAction()) {
                case NOTHING -> original.call(packet);
                case TARPIT -> {
                }
                case DISCONNECT -> connection.disconnect(Utils.translatable("multiplayer.disconnect.generic"));
                default -> {
                    AntiScan.LOGGER.error("Impossible case - login action not handled. Allowing connection. Please report this at https://github.com/skycatminepokie/antiscan/issues.");
                    original.call(packet);
                }
            }
            if (AntiScan.CONFIG.isLoginReport() && connection.getAddress() instanceof InetSocketAddress inetSocketAddress) {
                AntiScan.IP_CHECKER.report(inetSocketAddress.getHostString(), "Unsolicited connection attempt. Reported by AntiScan for Fabric.", new int[]{14});
            }
        }
    }
}
