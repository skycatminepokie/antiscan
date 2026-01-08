package com.skycatdev.antiscan.impl;

import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationListTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void ipWhitelistSucceedsOnMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, true);

        assertThat(whitelist.addBlocking(ip))
                .isTrue();

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void ipWhitelistPassesWhenEmpty() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, true);

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void ipWhitelistPassesOnNoMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        String toWhitelist = testUtil.newIp();
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, true);

        assertThat(whitelist.addBlocking(toWhitelist))
                .isTrue();

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void ipWhitelistPassesWhenRemoved() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, true);
        whitelist.addBlocking(ip);
        whitelist.removeBlocking(ip);

        assertThat(whitelist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void ipBlacklistFailsOnMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, true);

        assertThat(blacklist.addBlocking(ip))
                .isTrue();

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.FAIL);
    }

    @Test
    void ipBlacklistPassesWhenEmpty() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, true);

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void ipBlacklistPassesWhenNoMatch() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        String toBlacklist = testUtil.newIp();
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, true);

        assertThat(blacklist.addBlocking(toBlacklist))
                .isTrue();

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void ipBlacklistPassesWhenRemoved() {
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, true);
        blacklist.addBlocking(ip);
        blacklist.removeBlocking(ip);

        assertThat(blacklist.check(connection, null, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameBlacklistFailsOnMatch() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, false);

        assertThat(blacklist.addBlocking(name))
                .isTrue();

        assertThat(blacklist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.FAIL);
    }

    @Test
    void nameBlacklistPassesWhenEmpty() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, false);

        assertThat(blacklist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameBlacklistPassesWhenNoMatch() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        String toBlacklist = "cow";
        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, false);

        assertThat(blacklist.addBlocking(toBlacklist))
                .isTrue();

        assertThat(blacklist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameBlacklistPassesWhenRemoved() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());

        VerificationList blacklist = new VerificationList(new HashSet<>(), VerificationStatus.FAIL, false);
        blacklist.addBlocking(name);
        blacklist.removeBlocking(name);

        assertThat(blacklist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameWhitelistSucceedsOnMatch() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, false);

        assertThat(whitelist.addBlocking(name))
                .isTrue();

        assertThat(whitelist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.SUCCEED);
    }

    @Test
    void nameWhitelistPassesWhenEmpty() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, false);

        assertThat(whitelist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameWhitelistPassesWhenNoMatch() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());
        String toWhitelist = "cow";
        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, false);

        assertThat(whitelist.addBlocking(toWhitelist))
                .isTrue();

        assertThat(whitelist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

    @Test
    void nameWhitelistPassesWhenRemoved() {
        String name = "hi";
        Connection connection = testUtil.mockConnection(testUtil.newIp());

        VerificationList whitelist = new VerificationList(new HashSet<>(), VerificationStatus.SUCCEED, false);
        whitelist.addBlocking(name);
        whitelist.removeBlocking(name);

        assertThat(whitelist.check(connection, name, Runnable::run))
                .succeedsWithin(Duration.ofNanos(0))
                .isEqualTo(VerificationStatus.PASS);
    }

}