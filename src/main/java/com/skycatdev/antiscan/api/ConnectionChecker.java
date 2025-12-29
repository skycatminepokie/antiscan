package com.skycatdev.antiscan.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

public interface ConnectionChecker {
    Codec<ConnectionChecker> CODEC = ConnectionCheckerType.REGISTRY.byNameCodec()
            .dispatch("type", ConnectionChecker::getType, ConnectionCheckerType::codec);

    VerificationStatus check(Connection connection, @Nullable String playerName);

    ConnectionCheckerType<?> getType();
}
