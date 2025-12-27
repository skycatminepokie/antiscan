package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.Utils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class StatusMixin {
    @Shadow
    @Final
    private Connection connection;

    @WrapMethod(method = "handlePingRequest")
    private void antiscan$handleBaddiesPing(ServerboundPingRequestPacket packet, Operation<Void> original) {
        Utils.handleIpConnection(connection, Antiscan.CONFIG.getPingMode(), Antiscan.CONFIG.getPingAction(), Antiscan.CONFIG.isPingReport(), () -> original.call(packet));
    }

    @WrapMethod(method = "handleStatusRequest")
    private void antiscan$handleBaddiesQuery(ServerboundStatusRequestPacket packet, Operation<Void> original) {
        Utils.handleIpConnection(connection, Antiscan.CONFIG.getQueryMode(), Antiscan.CONFIG.getQueryAction(), Antiscan.CONFIG.isQueryReport(), () -> original.call(packet));
    }
}
