package com.skycatdev.antiscan.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class IpWhitelistChecker implements ConnectionChecker {
    public static final MapCodec<IpWhitelistChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("whitelist").forGetter(IpWhitelistChecker::exportWhitelist)
    ).apply(instance, IpWhitelistChecker::new));

    private final HashSet<String> whitelist;
    private final Object lock = new Object[]{};

    public IpWhitelistChecker(List<String> whitelist) {
        this.whitelist = new HashSet<>(whitelist);
    }

    public IpWhitelistChecker(HashSet<String> whitelist) {
        this.whitelist = whitelist;
    }

    private List<String> exportWhitelist() {
        synchronized (lock) {
            return new LinkedList<>(whitelist);
        }
    }

    public Future<Boolean> add(String ip, Executor executor) {
        return CompletableFuture.supplyAsync(() -> addBlocking(ip), executor);
    }

    public boolean addBlocking(String ip) {
        synchronized (lock) {
            return whitelist.add(ip);
        }
    }

    public Future<Boolean> remove(String ip, Executor executor) {
        return CompletableFuture.supplyAsync(() -> removeBlocking(ip), executor);
    }

    public boolean removeBlocking(String ip) {
        synchronized (lock) {
            return whitelist.remove(ip);
        }
    }

    public Future<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
            return CompletableFuture.supplyAsync(() -> {
                synchronized (lock) {
                    if (whitelist.contains(socketAddress.getAddress().getHostAddress())) {
                        return VerificationStatus.SUCCEED;
                    }
                }
                return VerificationStatus.PASS;
            }, executor);
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.IP_WHITELIST;
    }
}
