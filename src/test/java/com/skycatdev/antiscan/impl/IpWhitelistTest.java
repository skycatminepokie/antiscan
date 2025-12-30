package com.skycatdev.antiscan.impl;

import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class IpWhitelistTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void succeedsOnMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        IpWhitelist whitelist = new IpWhitelist(new HashSet<>());

        assertThat(whitelist.addBlocking(ip))
                .isTrue();

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void passesWhenEmpty() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        IpWhitelist whitelist = new IpWhitelist(new HashSet<>());

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void passesWhenNoMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        String toWhitelist = testUtil.newIp();
        IpWhitelist whitelist = new IpWhitelist(new HashSet<>());

        assertThat(whitelist.addBlocking(toWhitelist))
                .isTrue();

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void passesWhenRemoved() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        IpWhitelist whitelist = new IpWhitelist(new HashSet<>());
        whitelist.addBlocking(ip);
        whitelist.removeBlocking(ip);

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

}