package com.skycatdev.antiscan.impl.checker;
import com.google.common.net.InetAddresses;
import com.mojang.serialization.DataResult;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NetworkFeedsTest {
    @TempDir Path temp;
    String drop(String cidr) { return "{\"cidr\":\"" + cidr + "\"}\n{\"type\":\"metadata\",\"records\":1,\"copyright\":\"test copyright\",\"terms\":\"https://www.spamhaus.org/drop/terms/\",\"timestamp\":" + System.currentTimeMillis()/1000 + "}"; }
    @Test void dropBlocksBothFamiliesAndExpires() {
        var feeds = new NetworkFeeds(temp);
        feeds.install("drop-v4", drop("192.0.2.0/24"), System.currentTimeMillis());
        feeds.install("drop-v6", drop("2001:db8::/32"), System.currentTimeMillis());
        assertThat(feeds.blocked(InetAddresses.forString("192.0.2.255"))).isTrue();
        assertThat(feeds.blocked(InetAddresses.forString("2001:db8::1"))).isTrue();
        assertThat(feeds.blocked(InetAddresses.forString("192.0.3.0"))).isFalse();
        feeds.install("drop-v4", drop("192.0.2.0/24"), System.currentTimeMillis() - FeedLimits.MAX_FEED_AGE);
        assertThat(feeds.blocked(InetAddresses.forString("192.0.2.1"))).isFalse();
    }
    @Test void ipsumObservesOnlyAndRejectsMalformedFeedsWithoutReplacingGood() {
        var feeds = new NetworkFeeds(temp);
        feeds.install("ipsum", "# comment\n192.0.2.1\n192.0.2.1\n", System.currentTimeMillis());
        feeds.observe(InetAddresses.forString("192.0.2.1")); feeds.observe(InetAddresses.forString("192.0.2.1"));
        assertThat(feeds.blocked(InetAddresses.forString("192.0.2.1"))).isFalse();
        assertThat(FeedLimits.parseIps("192.0.2.1\n192.0.2.1")).hasSize(1);
        feeds.install("drop-v4", drop("192.0.2.0/24"), System.currentTimeMillis());
        for (String malformed : new String[]{"<html>error</html>", "{\"cidr\":\"192.0.2.0/24\"}", drop("not-a-cidr"), drop("::/0")})
            assertThatThrownBy(() -> feeds.install("drop-v4", malformed, System.currentTimeMillis())).isInstanceOf(RuntimeException.class);
        assertThat(feeds.blocked(InetAddresses.forString("192.0.2.1"))).isTrue();
        assertThatThrownBy(() -> FeedLimits.parseIps("192.0.2.1\n<html>")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void snapshotsSurviveRestartWithAttribution() throws Exception {
        var feeds = new NetworkFeeds(temp);
        try (var http = mockStatic(Utils.class, CALLS_REAL_METHODS)) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenAnswer(invocation -> {
                java.net.http.HttpRequest request = invocation.getArgument(0);
                HttpResponse<String> response = mock();
                when(response.body()).thenReturn(request.uri().toString().contains("drop_v4") ? drop("192.0.2.0/24") : request.uri().toString().contains("drop_v6") ? drop("2001:db8::/32") : "192.0.2.1");
                return DataResult.success(response);
            });
            feeds.refresh();
        }
        assertThat(Files.readString(temp.resolve("drop-v4.json"))).contains("copyright", "terms", "timestamp", "spamhaus.org");
        var restored = new NetworkFeeds(temp); restored.load();
        assertThat(restored.blocked(InetAddresses.forString("192.0.2.1"))).isTrue();
        assertThat(restored.blocked(InetAddresses.forString("2001:db8::1"))).isTrue();
        try (var http = mockStatic(Utils.class)) {
            http.when(() -> Utils.sendHttpRequest(any(), any())).thenReturn(DataResult.error(() -> "offline"));
            restored.refresh();
            assertThat(restored.blocked(InetAddresses.forString("192.0.2.1"))).isTrue();
        }
    }
}
