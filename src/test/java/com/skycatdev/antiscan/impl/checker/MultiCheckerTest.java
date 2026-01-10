package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.JsonOps;
import com.skycatdev.antiscan.api.ConnectionChecker;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiCheckerTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void zeroCheckersPass() {
        MultiChecker checker = new MultiChecker(List.of());
        Connection connection = mock();

        assertThat(checker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void oneCheckerEqual() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        ConnectionChecker backingChecker = mock();
        when(backingChecker.check(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.FAIL))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.PASS))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.SUCCEED));
        MultiChecker multiChecker = new MultiChecker(List.of(backingChecker));

        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.FAIL);
        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.PASS);
        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void testTwoPriorities() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        ConnectionChecker backing1 = mock();
        when(backing1.check(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.PASS))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.FAIL))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.SUCCEED));
        ConnectionChecker backing2 = mock();
        when(backing2.check(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.FAIL))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.SUCCEED))
                .thenReturn(CompletableFuture.completedFuture(VerificationStatus.PASS));
        MultiChecker multiChecker = new MultiChecker(List.of(backing1, backing2));

        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.FAIL);
        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.SUCCEED);
        assertThat(multiChecker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofMillis(0))
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void testSaveLoad() {
        MultiChecker checker = new MultiChecker(List.of(LocalChecker.INSTANCE, new VerificationList(VerificationStatus.FAIL, true)));
        var json = ConnectionChecker.CODEC.encode(checker, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow();
        var loaded = ConnectionChecker.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
        assertThat(loaded)
                .isInstanceOf(MultiChecker.class);
    }

}