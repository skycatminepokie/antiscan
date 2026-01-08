package com.skycatdev.antiscan.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface ConnectionChecker {
    Codec<ConnectionChecker> CODEC = ConnectionCheckerType.REGISTRY.byNameCodec()
            .dispatch("type", ConnectionChecker::getType, ConnectionCheckerType::codec);

    CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor);

    ConnectionCheckerType<?> getType();
}
