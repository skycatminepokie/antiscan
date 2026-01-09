package com.skycatdev.antiscan.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import net.minecraft.core.Registry;

import java.util.function.Function;

public class ConnectionCheckers {
    public static final ConnectionCheckerType<VerificationList> LIST = register("list", VerificationList.CODEC);
    public static final ConnectionCheckerType<LocalChecker> LOCAL = register("local", LocalChecker.CODEC);
    public static final ConnectionCheckerType<HunterChecker> HUNTER = register("hunter", HunterChecker.CODEC);
    public static final ConnectionCheckerType<AbuseIpdbChecker> ABUSE_IPDB = register("abuse_ipdb", AbuseIpdbChecker.CODEC);
    public static final ConnectionCheckerType<MultiChecker> MULTI = register("multi",
            checkerCodec -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                    checkerCodec.listOf().fieldOf("checkers").forGetter(MultiChecker::getCheckers)
            ).apply(instance, MultiChecker::new)));

    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }

    public static <T extends ConnectionChecker> ConnectionCheckerType<T> register(String id, MapCodec<T> codec) {
        return Registry.register(ConnectionCheckerType.REGISTRY, Antiscan.locate(id), new ConnectionCheckerType<>(codec));
    }

    public static <T extends ConnectionChecker> ConnectionCheckerType<T> register(String id, Function<Codec<ConnectionChecker>, MapCodec<T>> codecFunction) {
        return Registry.register(ConnectionCheckerType.REGISTRY, Antiscan.locate(id), new ConnectionCheckerType<>(codecFunction));
    }
}
