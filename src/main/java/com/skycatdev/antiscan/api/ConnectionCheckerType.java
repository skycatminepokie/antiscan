package com.skycatdev.antiscan.api;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.Antiscan;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public record ConnectionCheckerType<T extends ConnectionChecker>(MapCodec<T> codec) {
    public static final Registry<ConnectionCheckerType<?>> REGISTRY = new MappedRegistry<>(
            ResourceKey.createRegistryKey(Antiscan.locate("connection_checker_type")),
            Lifecycle.stable()
    );
}
