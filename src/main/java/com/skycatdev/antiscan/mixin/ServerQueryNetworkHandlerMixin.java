package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import com.skycatdev.antiscan.Utils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerQueryNetworkHandler.class)
public abstract class ServerQueryNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    //? if >=1.20.2
    @WrapMethod(method = "onQueryPing")
    //? if <1.20.2
    /*@WrapMethod(method = "onPing")*/
    private void antiScan$tarpitBaddiesPing(QueryPingC2SPacket packet, Operation<Void> original) {
        Utils.handleIpConnection(AntiScan.CONFIG.getPingMode(), AntiScan.CONFIG.getPingAction(), AntiScan.CONFIG.isPingReport(), connection, () -> original.call(packet));
    }

    @WrapMethod(method = "onRequest")
    private void antiScan$tarpitBaddiesQuery(QueryRequestC2SPacket packet, Operation<Void> original) {
        Utils.handleIpConnection(AntiScan.CONFIG.getQueryMode(), AntiScan.CONFIG.getQueryAction(), AntiScan.CONFIG.isQueryReport(), connection, () -> original.call(packet));
    }
}
