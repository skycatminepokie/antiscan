package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VerificationList implements ConnectionChecker {
    public static final MapCodec<VerificationList> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("list").forGetter(VerificationList::exportList),
            VerificationStatus.CODEC.fieldOf("status_on_match").forGetter(VerificationList::getStatusOnMatch),
            Codec.BOOL.fieldOf("is_ip_list").forGetter(VerificationList::isIpList)
    ).apply(instance, VerificationList::new));
    private final HashSet<String> list;
    /**
     *
     */
    private final VerificationStatus statusOnMatch;
    /**
     * {@code false} is a username list
     */
    private final boolean isIpList;

    public VerificationList(VerificationStatus statusOnMatch, boolean isIpList) {
        this(new HashSet<>(), statusOnMatch, isIpList);
    }

    public VerificationList(List<String> list, VerificationStatus statusOnMatch, boolean isIpList) {
        this(new HashSet<>(list), statusOnMatch, isIpList);
    }

    public VerificationList(HashSet<String> list, VerificationStatus statusOnMatch, boolean isIpList) {
        this.list = list;
        this.statusOnMatch = statusOnMatch;
        this.isIpList = isIpList;
    }

    public CompletableFuture<Boolean> add(String str, Executor executor) {
        return CompletableFuture.supplyAsync(() -> addBlocking(str), executor);
    }

    public boolean addBlocking(String str) {
        synchronized (list) {
            return list.add(str);
        }
    }

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        if (isIpList) {
            if (connection.getRemoteAddress() instanceof InetSocketAddress socketAddress) {
                return CompletableFuture.supplyAsync(() -> {
                    synchronized (list) {
                        if (list.contains(socketAddress.getAddress().getHostAddress())) {
                            return statusOnMatch;
                        }
                    }
                    return VerificationStatus.PASS;
                }, executor);
            }
        } else {
            if (playerName != null) {
                return CompletableFuture.supplyAsync(() -> {
                    synchronized (list) {
                        if (list.contains(playerName)) {
                            return statusOnMatch;
                        }
                    }
                    return VerificationStatus.PASS;
                }, executor);
            }
        }
        return CompletableFuture.completedFuture(VerificationStatus.PASS);
    }

    protected List<String> exportList() {
        synchronized (list) {
            return new LinkedList<>(list);
        }
    }

    public CompletableFuture<ListPart> getPart(long size, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getPartNow(size), executor);
    }

    public ListPart getPartNow(long size) {
        synchronized (list) {
            var stream = list.stream();
            if (size != -1) {
                stream = stream.limit(size);
            }
            return new ListPart(stream.toList(), list.size());
        }
    }

    public void clear() {
        synchronized (list) {
            list.clear();
        }
    }

    public void addAll(Collection<String> toAdd) {
        synchronized (list) {
            list.addAll(toAdd);
        }
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.LIST;
    }

    public VerificationStatus getStatusOnMatch() {
        return statusOnMatch;
    }

    public boolean isIpList() {
        return isIpList;
    }

    public CompletableFuture<Boolean> remove(String str, Executor executor) {
        return CompletableFuture.supplyAsync(() -> removeBlocking(str), executor);
    }

    public boolean removeBlocking(String str) {
        synchronized (list) {
            return list.remove(str);
        }
    }

    public static record ListPart(List<String> part, int superSize) {

    }

}
