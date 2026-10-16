package com.skycatdev.antiscan.impl;
import com.google.common.net.InetAddresses;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CidrTest {
    boolean match(String cidr, String ip) { return Cidr.parse(cidr).contains(InetAddresses.forString(ip)); }
    @Test void ipv4BoundariesAndHostBits() {
        assertThat(match("192.0.2.123/24", "192.0.2.0")).isTrue();
        assertThat(match("192.0.2.0/24", "192.0.2.255")).isTrue();
        assertThat(match("192.0.2.0/24", "192.0.3.0")).isFalse();
        assertThat(match("0.0.0.0/0", "255.255.255.255")).isTrue();
        assertThat(match("192.0.2.1/32", "192.0.2.2")).isFalse();
    }
    @Test void ipv6BoundariesAndFamilies() {
        assertThat(match("2001:db8::/32", "2001:db8:ffff:ffff:ffff:ffff:ffff:ffff")).isTrue();
        assertThat(match("2001:db8::/32", "2001:db9::")).isFalse();
        assertThat(match("2001:db8::1/128", "2001:db8::1")).isTrue();
        assertThat(match("2001:db8::1/128", "2001:db8::2")).isFalse();
        assertThat(match("::/0", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isTrue();
        assertThat(match("::/0", "192.0.2.1")).isFalse();
        assertThat(match("2001:db8::/33", "2001:db8:8000::")).isFalse();
    }
    @Test void malformedNeverUsesDns() {
        for (String value : new String[]{"example.com/24", "192.0.2.1", "192.0.2.1/33", "::/129", "::/-1", "::/", "::/0/1", "999.0.0.1/24"})
            assertThatThrownBy(() -> Cidr.parse(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
