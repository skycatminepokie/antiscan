package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @WrapMethod(method = "onHello")
    private void antiScan$tarpitBaddies(LoginHelloC2SPacket packet, Operation<Void> original) {
        if (!AntiScan.NAME_CHECKER.isBlacklisted(packet.name())) {
            original.call(packet);
        } else {
            AntiScan.LOGGER.info("Tarpitting '{}'", packet.name());
        }
    }
}
