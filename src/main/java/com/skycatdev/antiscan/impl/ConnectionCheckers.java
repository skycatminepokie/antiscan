package com.skycatdev.antiscan.impl;

import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.core.Registry;

public class ConnectionCheckers {
    public static final ConnectionCheckerType<VerificationList> LIST = register("list", VerificationList.CODEC);
    public static final ConnectionCheckerType<LocalChecker> LOCAL = register("local", LocalChecker.CODEC);
    public static final ConnectionCheckerType<HunterChecker> HUNTER = register("hunter", HunterChecker.CODEC);

    public static void init() {
    }

    public static <T extends ConnectionChecker> ConnectionCheckerType<T> register(String id, MapCodec<T> codec) {
        return Registry.register(ConnectionCheckerType.REGISTRY, Antiscan.locate(id), new ConnectionCheckerType<>(codec));
    }
}
