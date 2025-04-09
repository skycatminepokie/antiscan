package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.net.InetSocketAddress;

@Mixin(ServerQueryNetworkHandler.class)
public abstract class ServerQueryNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    @WrapMethod(method = "onQueryPing")
    private void antiScan$tarpitBaddiesPing(QueryPingC2SPacket packet, Operation<Void> original) {
        if (connection.isLocal() || !(connection.getAddress() instanceof InetSocketAddress inetSocketAddress) || inetSocketAddress.getHostName().equals("127.0.0.1") || !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName())) {
            original.call(packet);
        } else {
            AntiScan.LOGGER.info("Tarpitting ping request from {}", connection.getAddressAsString(true));
        }
    }

    @WrapMethod(method = "onRequest")
    private void antiScan$tarpitBaddiesQuery(QueryRequestC2SPacket packet, Operation<Void> original) {
        if (connection.isLocal() || !(connection.getAddress() instanceof InetSocketAddress inetSocketAddress) || inetSocketAddress.getHostName().equals("127.0.0.1") || !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostName())) {
            original.call(packet);
        } else {
            AntiScan.LOGGER.info("Tarpitting query request from {}", connection.getAddressAsString(true));
        }
    }
}
