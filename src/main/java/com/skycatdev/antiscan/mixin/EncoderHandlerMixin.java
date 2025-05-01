package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.skycatdev.antiscan.AntiScan;
import io.netty.channel.ChannelHandlerContext;
//? if >=1.20.5
import net.minecraft.network.handler.EncoderHandler;
//? if <1.20.5 && >1.20.1
/*import net.minecraft.network.handler.PacketEncoder;*/
//? if <=1.20.1
/*import net.minecraft.network.PacketEncoder;*/
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;

//? if >=1.20.5
@Mixin(EncoderHandler.class)
//? if <1.20.5
/*@Mixin(PacketEncoder.class)*/
public abstract class EncoderHandlerMixin {

    @WrapOperation(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"), remap = false)
    private void antiScan$supressErrors(Logger instance, String s, Object o1, Object o2, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) ChannelHandlerContext context) {
        if (!(context.channel().remoteAddress() instanceof InetSocketAddress inetSocketAddress) || !AntiScan.IP_CHECKER.isBlacklisted(inetSocketAddress.getHostString())) {
            original.call(instance, s, o1, o2);
        }
    }
}
