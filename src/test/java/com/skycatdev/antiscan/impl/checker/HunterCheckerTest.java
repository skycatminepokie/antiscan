package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.DataResult;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import com.skycatdev.antiscan.test.TestUtil;
import org.junit.jupiter.api.Test;
import java.net.http.HttpResponse;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HunterCheckerTest {
    TestUtil util = new TestUtil();
    @Test void admissionNeverRefreshesEvenWithDirectExecutor() {
        HunterChecker checker = new HunterChecker();
        try (var http = mockStatic(Utils.class)) {
            assertThat(checker.check(util.mockConnection(), null, Runnable::run).join()).isEqualTo(VerificationStatus.PASS);
            http.verifyNoInteractions();
        }
    }
    @Test void refreshAndCooldown() {
        HunterChecker checker = new HunterChecker();
        String ip = util.newIp();
        HttpResponse<String> response = mock(); when(response.body()).thenReturn(ip);
        try (var http = mockStatic(Utils.class)) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.success(response));
            assertThat(checker.updateIfNeeded()).isTrue();
            assertThat(checker.check(util.mockConnection(ip), null, Runnable::run).join()).isEqualTo(VerificationStatus.FAIL);
            checker.updateIfNeeded();
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(1));
        }
    }
    @Test void failuresAndMalformedUpdatesRetainLastGood() {
        String ip = util.newIp();
        HunterChecker checker = new HunterChecker(List.of(ip), System.currentTimeMillis(), 0);
        HttpResponse<String> response = mock(); when(response.body()).thenReturn("<html>unavailable</html>");
        try (var http = mockStatic(Utils.class)) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "offline"));
            assertThat(checker.updateNow()).isFalse();
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.success(response));
            assertThat(checker.updateNow()).isFalse();
            assertThat(checker.check(util.mockConnection(ip), null, Runnable::run).join()).isEqualTo(VerificationStatus.FAIL);
        }
    }
    @Test void staleFeedExpires() {
        String ip = util.newIp();
        HunterChecker checker = new HunterChecker(List.of(ip), System.currentTimeMillis() - FeedLimits.MAX_FEED_AGE, 0);
        assertThat(checker.check(util.mockConnection(ip), null, Runnable::run).join()).isEqualTo(VerificationStatus.PASS);
    }
}
