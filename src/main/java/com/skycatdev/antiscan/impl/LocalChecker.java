package com.skycatdev.antiscan.impl;

import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LocalChecker implements ConnectionChecker {
    public static final LocalChecker INSTANCE = new LocalChecker();
    public static final MapCodec<LocalChecker> CODEC = MapCodec.unit(INSTANCE);

    private LocalChecker() {

    }

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        if (connection.getRemoteAddress()  instanceof InetSocketAddress socketAddress) {
            // Omitting == InetAddress.getLocalHost because that seems unnecessary,
            // especially since the cache for it is short, and it looks like a decently long process.
            if (socketAddress.getAddress().isLoopbackAddress() ||
                socketAddress.getAddress().isSiteLocalAddress()) {
                return CompletableFuture.completedFuture(VerificationStatus.SUCCEED);
            }
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.LOCAL;
    }
}
