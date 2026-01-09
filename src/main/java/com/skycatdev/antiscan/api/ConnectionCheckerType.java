package com.skycatdev.antiscan.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.Antiscan;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

public record ConnectionCheckerType<T extends ConnectionChecker>(Function<Codec<ConnectionChecker>, MapCodec<T>> codecFunction) {
    public static final Registry<ConnectionCheckerType<?>> REGISTRY = new MappedRegistry<>(
            ResourceKey.createRegistryKey(Antiscan.locate("connection_checker_type")),
            Lifecycle.stable()
    );

    public ConnectionCheckerType(MapCodec<T> codec) {
        this(ignored -> codec);
    }
}
