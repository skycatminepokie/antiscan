package com.skycatdev.antiscan.impl;

import com.mojang.serialization.MapCodec;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import net.minecraft.core.Registry;

public class ConnectionCheckers {
    public static final ConnectionCheckerType<IpBlacklist> IP_BLACKLIST = register("ip_blacklist", IpBlacklist.CODEC);
    public static final ConnectionCheckerType<IpWhitelist> IP_WHITELIST = register("ip_whitelist", IpWhitelist.CODEC);

    public static void init() {
    }

    public static <T extends ConnectionChecker> ConnectionCheckerType<T> register(String id, MapCodec<T> codec) {
        return Registry.register(ConnectionCheckerType.REGISTRY, Antiscan.locate(id), new ConnectionCheckerType<>(codec));
    }
}
