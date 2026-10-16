package com.skycatdev.antiscan.impl.checker;

import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.impl.Config;
import com.skycatdev.antiscan.impl.ConnectionRates;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import com.skycatdev.antiscan.test.TestUtil;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdmissionTest {
    TestUtil util = new TestUtil();

    @Test void whitelistAndLocalOverrideBlacklistAndTemporaryBan() {
        String ip = "198.51.100.42";
        var connection = util.mockConnection(ip);
        var allowed = new AtomicInteger();
        Antiscan.CONFIG.ipBlacklist().addBlocking(ip);
        Antiscan.CONFIG.ipWhitelist().addBlocking(ip);
        for (int i = 0; i < 61; i++) Antiscan.RATES.blocked(ip, new Object(), ConnectionRates.Kind.LOGIN, System.currentTimeMillis());
        try {
            Antiscan.handleConnection(connection, null, allowed::incrementAndGet, ConnectionRates.Kind.LOGIN);
            assertThat(allowed.get()).isEqualTo(1);
            verify(connection, never()).disconnect(any(net.minecraft.network.chat.Component.class));
            Antiscan.CONFIG.ipBlacklist().addBlocking("127.0.0.1");
            Antiscan.handleConnection(util.mockConnection("127.0.0.1"), null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(2);
        } finally {
            Antiscan.CONFIG.ipBlacklist().removeBlocking(ip);
            Antiscan.CONFIG.ipWhitelist().removeBlocking(ip);
            Antiscan.CONFIG.ipBlacklist().removeBlocking("127.0.0.1");
            Antiscan.RATES.unban(ip);
        }
    }

    static final String PUBLIC_IP = "198.51.100.50";

    @Test void publicAddressOverRateLimitIsDisconnectedAndOrdinaryOnesAreAllowed() {
        var allowed = new AtomicInteger();
        try {
            for (int i = 0; i < 60; i++) {
                Antiscan.handleConnection(util.mockConnection(PUBLIC_IP), null, allowed::incrementAndGet, ConnectionRates.Kind.LOGIN);
            }
            assertThat(allowed.get()).isEqualTo(60);
            var over = util.mockConnection(PUBLIC_IP);
            Antiscan.handleConnection(over, null, allowed::incrementAndGet, ConnectionRates.Kind.LOGIN);
            assertThat(allowed.get()).isEqualTo(60);
            verify(over).disconnect(any(net.minecraft.network.chat.Component.class));
            // the ban applies to every later connection from that address
            var later = util.mockConnection(PUBLIC_IP);
            Antiscan.handleConnection(later, null, allowed::incrementAndGet);
            verify(later).disconnect(any(net.minecraft.network.chat.Component.class));
            // a different address is unaffected
            Antiscan.handleConnection(util.mockConnection("198.51.100.51"), null, allowed::incrementAndGet, ConnectionRates.Kind.LOGIN);
            assertThat(allowed.get()).isEqualTo(61);
        } finally {
            Antiscan.RATES.unban(PUBLIC_IP);
        }
    }

    @Test void rateLimitsCanBeTurnedOff() {
        var allowed = new AtomicInteger();
        var original = Antiscan.protection;
        Antiscan.protection = new Config.Protection(false, 240, 180, 60, true);
        try {
            for (int i = 0; i < 100; i++) {
                Antiscan.handleConnection(util.mockConnection("198.51.100.52"), null, allowed::incrementAndGet, ConnectionRates.Kind.LOGIN);
            }
            assertThat(allowed.get()).isEqualTo(100);
        } finally {
            Antiscan.protection = original;
            Antiscan.RATES.unban("198.51.100.52");
        }
    }

    @Test void spamhausDropRangeDisconnectsUnlessFeedsAreDisabled() {
        String dropIp = "203.0.113.9";
        String body = "{\"cidr\":\"203.0.113.0/24\"}\n{\"type\":\"metadata\",\"records\":1,\"copyright\":\"test\",\"terms\":\"https://www.spamhaus.org/drop/terms/\",\"timestamp\":"
                + System.currentTimeMillis() / 1000 + "}";
        Antiscan.FEEDS.install("drop-v4", body, System.currentTimeMillis());
        var original = Antiscan.protection;
        try {
            var blocked = util.mockConnection(dropIp);
            var allowed = new AtomicInteger();
            Antiscan.handleConnection(blocked, null, allowed::incrementAndGet);
            verify(blocked).disconnect(any(net.minecraft.network.chat.Component.class));
            assertThat(allowed.get()).isZero();

            Antiscan.protection = new Config.Protection(true, 240, 180, 60, false);
            Antiscan.handleConnection(util.mockConnection(dropIp), null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(1);

            Antiscan.protection = original;
            Antiscan.handleConnection(util.mockConnection("203.0.114.9"), null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(2);
            Antiscan.FEEDS.install("drop-v4", body, System.currentTimeMillis() - FeedLimits.MAX_FEED_AGE); // stale feeds are ignored
            Antiscan.handleConnection(util.mockConnection(dropIp), null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(3);
        } finally {
            Antiscan.protection = original;
            Antiscan.FEEDS.install("drop-v4", body, System.currentTimeMillis() - FeedLimits.MAX_FEED_AGE);
        }
    }

    @Test void legacyFailReportNameAndIpDisconnectButNeverReport() {
        String ip = "198.51.100.43", name = "antiscan_test_blocked";
        var checker = Antiscan.CONFIG.abuseIpdbChecker();
        String oldKey = checker.key;
        checker.key = "fake-key";
        Antiscan.CONFIG.nameBlacklist().addBlocking(name);
        Antiscan.CONFIG.ipBlacklist().addBlocking(ip);
        try (var http = mockStatic(Utils.class)) {
            var byName = util.mockConnection("198.51.100.44");
            var byIp = util.mockConnection(ip);
            Runnable allowed = mock();
            Antiscan.handleConnection(byName, name, allowed);
            Antiscan.handleConnection(byIp, null, allowed);
            verify(byName).disconnect(any(net.minecraft.network.chat.Component.class)); verify(byIp).disconnect(any(net.minecraft.network.chat.Component.class));
            verifyNoInteractions(allowed);
            http.verifyNoInteractions();
        } finally {
            checker.key = oldKey;
            Antiscan.CONFIG.nameBlacklist().removeBlocking(name);
            Antiscan.CONFIG.ipBlacklist().removeBlocking(ip);
        }
    }
}
