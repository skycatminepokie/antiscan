package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import com.skycatdev.antiscan.Utils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerHandshakeNetworkHandler.class)
public abstract class ServerHandshakeNetworkHandlerMixin {

    @Shadow
    @Final
    private ClientConnection connection;

    @WrapMethod(method = "login")
    private void antiScan$tarpitBaddies(HandshakeC2SPacket packet, boolean transfer, Operation<Void> original) {
        Utils.handleIpConnection(AntiScan.CONFIG.getHandshakeMode(), AntiScan.CONFIG.getHandshakeAction(), AntiScan.CONFIG.isHandshakeReport(), connection, () -> original.call(packet, transfer));
    }
}
