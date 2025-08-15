package com.skycatdev.antiscan.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.antiscan.AntiScan;
import com.skycatdev.antiscan.Utils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    ClientConnection connection;

    @WrapMethod(method = "onHello")
    private void antiScan$handleBaddies(LoginHelloC2SPacket packet, Operation<Void> original) {
        Utils.handleNameIpConnection(connection, packet.name(), AntiScan.CONFIG.getLoginMode(), AntiScan.CONFIG.getLoginAction(), AntiScan.CONFIG.isLoginReport(), original::call);
    }
}
