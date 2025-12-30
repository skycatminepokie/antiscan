package com.skycatdev.antiscan.impl;

import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import net.minecraft.core.Registry;

public class ConnectionCheckers {
    public static final ConnectionCheckerType<IpBlacklistChecker> IP_BLACKLIST = register("ip_blacklist", IpBlacklistChecker.CODEC);
    public static final ConnectionCheckerType<IpWhitelistChecker> IP_WHITELIST = register("ip_whitelist", IpWhitelistChecker.CODEC);

    public static void init() {
    }

    public static <T extends ConnectionChecker> ConnectionCheckerType<T> register(String id, MapCodec<T> codec) {
        return Registry.register(ConnectionCheckerType.REGISTRY, Antiscan.locate(id), new ConnectionCheckerType<>(codec));
    }
}
