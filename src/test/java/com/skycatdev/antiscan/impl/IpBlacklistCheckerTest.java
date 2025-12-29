package com.skycatdev.antiscan.impl;

import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class IpBlacklistCheckerTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void failsOnMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        IpBlacklistChecker blacklist = new IpBlacklistChecker(new HashSet<>());

        assertThat(blacklist.addBlocking(ip))
                .isTrue();

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.FAIL);
    }

    @Test
    void passesWhenEmpty() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        IpBlacklistChecker blacklist = new IpBlacklistChecker(new HashSet<>());

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void passesWhenNoMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        String toBlacklist = testUtil.newIp();
        IpBlacklistChecker blacklist = new IpBlacklistChecker(new HashSet<>());

        assertThat(blacklist.addBlocking(toBlacklist))
                .isTrue();

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void passesWhenRemoved() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        IpBlacklistChecker blacklist = new IpBlacklistChecker(new HashSet<>());
        blacklist.addBlocking(ip);
        blacklist.removeBlocking(ip);

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

}