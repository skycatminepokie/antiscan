package com.skycatdev.antiscan.impl;

import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LocalCheckerTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void succeedsLocalhost() {
        LocalChecker checker = LocalChecker.INSTANCE;
        String ip = "127.0.0.1";
        Connection connection = testUtil.mockConnection(ip);

        assertThat(checker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ZERO)
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void succeedsPrivate() {
        LocalChecker checker = LocalChecker.INSTANCE;
        String ip = "192.168.0.0";
        Connection connection = testUtil.mockConnection(ip);

        assertThat(checker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ZERO)
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void passesNonLocal() {
        LocalChecker checker = LocalChecker.INSTANCE;
        Connection connection = testUtil.mockConnection();

        assertThat(checker.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ZERO)
                .isEqualTo(VerificationStatus.PASS);
    }
}