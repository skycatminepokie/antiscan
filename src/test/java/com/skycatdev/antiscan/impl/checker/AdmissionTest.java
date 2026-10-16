package com.skycatdev.antiscan.impl.checker;

import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import com.skycatdev.antiscan.test.TestUtil;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdmissionTest {
    TestUtil util = new TestUtil();

    @Test void whitelistAndLocalOverrideBlacklist() {
        String ip = "198.51.100.42";
        var connection = util.mockConnection(ip);
        var allowed = new AtomicInteger();
        Antiscan.CONFIG.ipBlacklist().addBlocking(ip);
        Antiscan.CONFIG.ipWhitelist().addBlocking(ip);
        try {
            Antiscan.handleConnection(connection, null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(1);
            verify(connection, never()).disconnect(any(net.minecraft.network.chat.Component.class));
            Antiscan.CONFIG.ipBlacklist().addBlocking("127.0.0.1");
            Antiscan.handleConnection(util.mockConnection("127.0.0.1"), null, allowed::incrementAndGet);
            assertThat(allowed.get()).isEqualTo(2);
        } finally {
            Antiscan.CONFIG.ipBlacklist().removeBlocking(ip);
            Antiscan.CONFIG.ipWhitelist().removeBlocking(ip);
            Antiscan.CONFIG.ipBlacklist().removeBlocking("127.0.0.1");
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
