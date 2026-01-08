package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.Antiscan;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class HandshakeMixin {

    @Shadow
    @Final
    private Connection connection;

    //? if >=1.20.5 {
    @WrapMethod(method = "beginLogin")
    private void antiscan$handleBaddies(ClientIntentionPacket packet, boolean transfer, Operation<Void> original) {
        Antiscan.handleConnection(connection, null, () -> original.call(packet, transfer));
    }
    //?}
    //? if <1.20.5 {
    /*@WrapMethod(method = "handleIntention")
    private void antiscan$handleBaddies(ClientIntentionPacket packet, Operation<Void> original) {
        Antiscan.handleConnection(connection, null, () -> original.call(packet));
    }
    *///?}
}
