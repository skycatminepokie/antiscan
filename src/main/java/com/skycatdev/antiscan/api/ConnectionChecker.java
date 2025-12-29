package com.skycatdev.antiscan.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public interface ConnectionChecker {
    Codec<ConnectionChecker> CODEC = ConnectionCheckerType.REGISTRY.byNameCodec()
            .dispatch("type", ConnectionChecker::getType, ConnectionCheckerType::codec);

    Future<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor);

    ConnectionCheckerType<?> getType();
}
