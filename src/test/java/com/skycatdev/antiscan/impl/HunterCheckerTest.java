package com.skycatdev.antiscan.impl;

import com.mojang.serialization.DataResult;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.checker.HunterChecker;
import com.skycatdev.antiscan.test.TestUtil;
import net.minecraft.network.Connection;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HunterCheckerTest {
    TestUtil testUtil = new TestUtil();

    @Test
    void updatesOnFirstCall() {
        HunterChecker checker = new HunterChecker();
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);
        HttpResponse<String> response = mock();
        when(response.body()).thenReturn(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.success(response));
            checker.check(connection, null, Runnable::run);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()));
        }
    }

    @Test
    void failOpen() {
        HunterChecker checker = new HunterChecker();
        String ip = testUtil.newIp();
        Connection connection = testUtil.mockConnection(ip);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "Forced error."));
            assertThat(checker.check(connection, null, Runnable::run))
                    .succeedsWithin(Duration.ofNanos(0))
                    .isEqualTo(VerificationStatus.PASS);
            mockedUtils.verify(() -> Utils.sendHttpRequest(any(), any()));
        }
    }

    @Test
    void usesCooldown() {
        HunterChecker checker = new HunterChecker();
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

}