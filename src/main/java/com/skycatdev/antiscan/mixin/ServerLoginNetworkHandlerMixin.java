package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
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
        if (connection.isLocal() ||
            !(connection.getAddress() instanceof InetSocketAddress inetSocketAddress) ||
            inetSocketAddress.getHostString().equals("127.0.0.1") ||
            !AntiScan.NAME_CHECKER.isBlacklisted(packet.name())) {
            original.call(packet);
        } else {
            AntiScan.IP_CHECKER.report(inetSocketAddress.getHostString(), "Unsolicited connection attempt. Blocked by AntiScan for Fabric.", new int[]{14});
            AntiScan.LOGGER.info("Tarpitting '{}'", packet.name());
        }
    }
}
