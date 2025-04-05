package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ServerHandshakeNetworkHandlerMixin {

    @WrapMethod(method = "login")
    private void antiScan$wrapLogin(HandshakeC2SPacket packet, boolean transfer, Operation<Void> original) {
        if (packet.address().equals("localhost") || !AntiScan.IP_CHECKER.isBlacklisted(packet.address())) {
            original.call(packet, transfer);
        } else {
            AntiScan.LOGGER.info("Blocking connection from {}", packet.address());
        }
    }
}
