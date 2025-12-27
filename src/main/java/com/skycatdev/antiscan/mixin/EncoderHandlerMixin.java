package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.skycatdev.antiscan.Antiscan;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;

import net.minecraft.network.PacketEncoder;

@Mixin(PacketEncoder.class)
public abstract class EncoderHandlerMixin<T extends PacketListener> {

    @WrapOperation(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void antiScan$supressErrorLog(Logger instance, String s, Object o1, Object o2, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) ChannelHandlerContext context) {
        if (context.channel().remoteAddress() instanceof InetSocketAddress inetSocketAddress && Antiscan.CONNECTION_CHECKER.isBlacklisted(inetSocketAddress.getHostString())) {
            return;
        }
        original.call(instance, s, o1, o2);
    }
    @WrapMethod(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V")
    private void antiScan$suppressErrorThrow(ChannelHandlerContext context, Packet<T> packet, ByteBuf byteBuf, Operation<Void> original) throws Exception {
        @Nullable Exception toThrow = null;
        try {
            original.call(context, packet, byteBuf);
        } catch (Exception e) {
            if (context.channel().remoteAddress() instanceof InetSocketAddress inetSocketAddress && Antiscan.CONNECTION_CHECKER.isBlacklisted(inetSocketAddress.getHostString())) {
                return;
            }
            toThrow = e;
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }
}
