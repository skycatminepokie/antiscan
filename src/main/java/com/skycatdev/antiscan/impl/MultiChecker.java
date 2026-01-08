package com.skycatdev.antiscan.impl;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MultiChecker implements ConnectionChecker {
    // NOPUSH: Make sure this works without recursion problems
    public static final MapCodec<MultiChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConnectionChecker.CODEC.listOf().fieldOf("checkers").forGetter(MultiChecker::getCheckers)
    ).apply(instance, MultiChecker::new));

    /**
     * Must be safe for concurrent reading.
     */
    protected final List<ConnectionChecker> checkers;

    public MultiChecker(List<ConnectionChecker> checkers) {
        this.checkers = List.copyOf(checkers);
    }

    protected List<ConnectionChecker> getCheckers() {
        return checkers;
    }

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        CompletableFuture<VerificationStatus> future = CompletableFuture.completedFuture(VerificationStatus.PASS);
        for (ConnectionChecker checker : checkers) {
            future = future.thenCombineAsync(checker.check(connection, playerName, executor), VerificationStatus::chooseHigher, executor);
        }
        return future;
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.MULTI;
    }

}
