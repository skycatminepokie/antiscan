package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.DataResult;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.Utils;
import com.skycatdev.antiscan.impl.BackgroundTasks;
import com.skycatdev.antiscan.test.TestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbuseIpdbCheckerTest {
    @TempDir Path temp;
    TestUtil util = new TestUtil();
    AbuseIpdbChecker checker() {
        AbuseIpdbChecker checker;
        try (var keys = mockStatic(AbuseIpdbChecker.class)) {
            keys.when(AbuseIpdbChecker::loadKey).thenReturn("fake-key");
            checker = new AbuseIpdbChecker();
        }
        checker.useLedger(temp.resolve("budget.json"));
        return checker;
    }
    HttpResponse<String> response(String body) { HttpResponse<String> response = mock(); when(response.body()).thenReturn(body); return response; }
    MockedStatic<Utils> http() { return mockStatic(Utils.class, CALLS_REAL_METHODS); }

    @Test void firstUnknownAllowedThenCachedBadBlocksWithoutReporting() {
        var checker = checker(); var connection = util.mockConnection("192.0.2.1");
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("{\"data\":{\"abuseConfidenceScore\":100}}")));
            assertThat(checker.check(connection, null, Runnable::run).join()).isEqualTo(VerificationStatus.PASS);
            http.verify(() -> Utils.sendHttpRequest(any(), any()), never());
            checker.lookupNow("192.0.2.1");
            assertThat(checker.check(connection, null, Runnable::run).join()).isEqualTo(VerificationStatus.FAIL);
            http.verify(() -> Utils.sendHttpRequest(argThat(r -> r.method().equals("POST")), any()), never());
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(1));
        }
    }
    @Test void cleanResultCachedAndHasTtl() throws Exception {
        var checker = checker();
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("{\"data\":{\"abuseConfidenceScore\":0}}")));
            checker.lookupNow("192.0.2.1");
            assertThat(checker.check(util.mockConnection("192.0.2.1"), null, Runnable::run).join()).isEqualTo(VerificationStatus.PASS);
            var field = AbuseIpdbChecker.class.getDeclaredField("cache"); field.setAccessible(true);
            Object cached = ((Map<?, ?>)field.get(checker)).get("192.0.2.1");
            var expires = cached.getClass().getDeclaredMethod("expires"); expires.setAccessible(true);
            assertThat((Long)expires.invoke(cached)).isBetween(System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(1));
        }
    }
    @Test void errorsAndMalformedAreNotCachedClean() throws Exception {
        var checker = checker();
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "offline"));
            checker.lookupNow("192.0.2.1");
            var cache = AbuseIpdbChecker.class.getDeclaredField("cache"); cache.setAccessible(true);
            assertThat((Map<?, ?>)cache.get(checker)).isEmpty();
        }
        var other = checker(); other.useLedger(temp.resolve("other.json"));
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("nonsense")));
            other.lookupNow("192.0.2.2");
            var cache = AbuseIpdbChecker.class.getDeclaredField("cache"); cache.setAccessible(true);
            assertThat((Map<?, ?>)cache.get(other)).isEmpty();
        }
    }
    @Test void refreshPreservesLastGoodOnMalformedAndHttpFailure() {
        var checker = checker();
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("192.0.2.1")));
            assertThat(checker.updateIfNeeded()).isTrue();
            assertThat(checker.check(util.mockConnection("192.0.2.1"), null, Runnable::run).join()).isEqualTo(VerificationStatus.FAIL);
        }
        // A persisted last-good list remains usable after restart even if refresh fails.
        AbuseIpdbChecker restored;
        try (var keys = mockStatic(AbuseIpdbChecker.class)) {
            keys.when(AbuseIpdbChecker::loadKey).thenReturn("fake-key");
            restored = new AbuseIpdbChecker(new HashSet<>(List.of("192.0.2.1")), System.currentTimeMillis() - 100_000, 0);
        }
        restored.useLedger(temp.resolve("budget.json"));
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("not-an-ip")));
            assertThat(restored.updateIfNeeded()).isFalse();
            assertThat(restored.check(util.mockConnection("192.0.2.1"), null, Runnable::run).join()).isEqualTo(VerificationStatus.FAIL);
        }
    }
    @Test void reportCooldownAndBudgetSurviveRestart() {
        var checker = checker();
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> DataResult.success(response("{}")));
            assertThat(checker.reportNow("192.0.2.1")).isTrue();
            assertThat(checker().reportNow("192.0.2.1")).isFalse();
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(1));
        }
    }
    @Test void quotaAnd429BackoffPersist() {
        var checker = checker(); var response = response("{}");
        when(response.statusCode()).thenReturn(429);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Retry-After", List.of("7200")), (a,b) -> true));
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "quota", response));
            checker.lookupNow("192.0.2.1");
            checker().lookupNow("192.0.2.2");
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(1));
        }
    }
    @Test void noKeySendsNothing() {
        var checker = checker(); checker.key = null;
        try (var http = http()) {
            checker.lookupNow("192.0.2.1");
            assertThat(checker.reportNow("192.0.2.1")).isFalse();
            http.verify(() -> Utils.sendHttpRequest(any(), any()), never());
        }
    }
    @Test void blacklistDailyBudgetSurvivesRestartWithoutExhaustingLookups() {
        try (var http = http()) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation ->
                    DataResult.success(response("192.0.2.1")));
            for (int i = 0; i < 5; i++) assertThat(checker().updateIfNeeded()).isTrue();
            assertThat(checker().updateIfNeeded()).isFalse();
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(5));
            checker().lookupNow("192.0.2.2");
            http.verify(() -> Utils.sendHttpRequest(any(), any()), times(6));
        }
    }
    @Test void failReportsAreOnlySentWhenAutoReportIsOn() throws Exception {
        var reported = new CopyOnWriteArrayList<String>();
        var latch = new CountDownLatch(1);
        class Recording extends AbuseIpdbChecker {
            Recording(boolean auto) { super(new HashSet<String>(), 0, 0, 0, 90, auto); }
            @Override public boolean reportNow(String ip) { reported.add(ip); latch.countDown(); return true; }
        }
        BackgroundTasks.start(() -> {});
        try {
            new Recording(false).onFailReport("192.0.2.1");
            Thread.sleep(200);
            assertThat(reported).isEmpty();
            new Recording(true).onFailReport("192.0.2.2");
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(reported).containsExactly("192.0.2.2");
        } finally { BackgroundTasks.stop(); }
    }
    @Test void admissionReturnsWhileLookupIsBlocked() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var checker = new AbuseIpdbChecker() {
            @Override protected void lookupNow(String ip) {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        };
        checker.key = "fake-key";
        BackgroundTasks.start(() -> {});
        try {
            assertThat(checker.check(util.mockConnection("192.0.2.1"), null, Runnable::run).join()).isEqualTo(VerificationStatus.PASS);
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(checker.check(util.mockConnection("192.0.2.2"), null, Runnable::run).isDone()).isTrue();
        } finally { release.countDown(); BackgroundTasks.stop(); }
    }
}
