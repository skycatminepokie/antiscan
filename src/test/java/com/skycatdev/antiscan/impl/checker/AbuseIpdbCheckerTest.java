package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.DataResult;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.Utils;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbuseIpdbCheckerTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void updatesOnFirstCall() {
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn("fakekey");
            checker = new AbuseIpdbChecker();
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response = mock();
        when(response.body()).thenReturn(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.success(response));

            checker.check(connection, null, Runnable::run);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()), times(1)); // once for blacklist
        }
    }

    @Test
    void usesGreylist() {
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn("fakekey");
            checker = new AbuseIpdbChecker();
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response1 = mock();
        when(response1.body()).thenReturn("nonsense");
        HttpResponse<String> response2 = mock();
        when(response2.body()).thenReturn("{\"data\":{\"abuseConfidenceScore\":100}}");

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any()))
                    .thenReturn(DataResult.success(response1))
                    .thenReturn(DataResult.success(response2));

            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofMillis(0))
                    .isEqualTo(VerificationStatus.FAIL);
            // one blacklist, one check (which is reported abusive)
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()), times(2));
        }
    }

    @Test
    void blacklistUpdates() {
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn("fakekey");
            checker = new AbuseIpdbChecker(new HashSet<>(), 0, -1);
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any()))
                    .thenAnswer((Answer<DataResult<HttpResponse<String>>>) invocation -> {
                        HttpResponse<String> response1 = mock();
                        when(response1.body()).thenReturn(ip);
                        return DataResult.success(response1);
                    })
                    .thenAnswer((Answer<DataResult<HttpResponse<String>>>) invocation -> {
                        HttpResponse<String> response2 = mock();
                        when(response2.body()).thenReturn("nonsense");
                        return DataResult.success(response2);
                    }).thenAnswer((Answer<DataResult<HttpResponse<String>>>) invocation -> {
                        HttpResponse<String> response3 = mock();
                        when(response3.body()).thenReturn("{\"data\":{\"abuseConfidenceScore\":0}}");
                        return DataResult.success(response3);
                    });

            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofMillis(0))
                    .isEqualTo(VerificationStatus.FAIL);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any())); // 1 request sent (blacklist update)
            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofMillis(0))
                    .isEqualTo(VerificationStatus.PASS);
            // 3 requests sent (previous blacklist update, this blacklist update, ip check)
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()), times(3));
        }
    }

    @Test
    void usesCooldown() {
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn("fakekey");
            checker = new AbuseIpdbChecker();
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response = mock();
        when(response.body()).thenReturn(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer((Answer<DataResult<HttpResponse<String>>>) invocation -> {
                Antiscan.LOGGER.info("Sent http request");
                return DataResult.success(response);
            });

            checker.check(connection, null, Runnable::run);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any())); // 1 request sent
            checker.check(connection, null, Runnable::run);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any())); // Still only 1 request sent
        }
    }

    @Test
    void noKeyBehavior() { // Fail open, no requests sent
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn(null);
            checker = new AbuseIpdbChecker();
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response = mock();
        when(response.body()).thenReturn(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenThrow(new AssertionError("No requests should be sent, but one was!"));

            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofMillis(0))
                    .isEqualTo(VerificationStatus.PASS);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()), never());
        }
    }

    @Test
    void failOpenWhenHttpError() {
        AbuseIpdbChecker checker;
        try (MockedStatic<AbuseIpdbChecker> mockedChecker = mockStatic(AbuseIpdbChecker.class)) {
            mockedChecker.when(AbuseIpdbChecker::loadKey).thenReturn("fakekey");
            checker = new AbuseIpdbChecker();
        }
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response = mock();
        when(response.body()).thenReturn(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "Forced error"));

            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofMillis(0))
                    .isEqualTo(VerificationStatus.PASS);
            // 1 request for blacklist, 1 for check
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()), times(2));
        }
    }
}