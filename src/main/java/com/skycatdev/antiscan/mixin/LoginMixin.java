package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.Antiscan;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class LoginMixin {
    @Shadow
    @Final
    Connection connection;

    @WrapMethod(method = "handleHello")
    private void antiscan$handleBaddies(ServerboundHelloPacket packet, Operation<Void> original) {
        Antiscan.handleConnection(connection, packet.name(), () -> original.call(packet));
    }
}
