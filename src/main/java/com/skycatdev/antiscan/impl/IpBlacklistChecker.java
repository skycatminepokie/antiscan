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

public class IpBlacklistChecker implements ConnectionChecker {
    public static final MapCodec<IpBlacklistChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("blacklist").forGetter(IpBlacklistChecker::exportBlacklist)
    ).apply(instance, IpBlacklistChecker::new));

    private final HashSet<String> blacklist;
    private final Object lock = new Object[]{};

    public IpBlacklistChecker(List<String> blacklist) {
        this.blacklist = new HashSet<>(blacklist);
    }

    public IpBlacklistChecker(HashSet<String> blacklist) {
        this.blacklist = blacklist;
    }

    private List<String> exportBlacklist() {
        synchronized (lock) {
            return new LinkedList<>(blacklist);
        }
    }

    public Future<Boolean> add(String ip, Executor executor) {
        return CompletableFuture.supplyAsync(() -> addBlocking(ip), executor);
    }

    public boolean addBlocking(String ip) {
        synchronized (lock) {
            return blacklist.add(ip);
        }
    }

    public Future<Boolean> remove(String ip, Executor executor) {
        return CompletableFuture.supplyAsync(() -> removeBlocking(ip), executor);
    }

    public boolean removeBlocking(String ip) {
        synchronized (lock) {
            return blacklist.remove(ip);
        }
    }

    public Future<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
            return CompletableFuture.supplyAsync(() -> {
                synchronized (lock) {
                    if (blacklist.contains(socketAddress.getAddress().getHostAddress())) {
                        return VerificationStatus.FAIL;
                    }
                }
                return VerificationStatus.PASS;
            }, executor);
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.IP_BLACKLIST;
    }
}
