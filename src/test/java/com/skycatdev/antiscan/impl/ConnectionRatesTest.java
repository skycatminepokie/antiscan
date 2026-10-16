package com.skycatdev.antiscan.impl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;
class ConnectionRatesTest {
    @TempDir Path temp;
    long now = System.currentTimeMillis();
    ConnectionRates rates() { return new ConnectionRates(temp.resolve("bans.json")); }
    @Test void statusPlusPingAndHandshakePlusHelloAreCountedOnce() {
        var rates = rates();
        for (int i = 0; i < 180; i++) {
            Object connection = new Object();
            assertThat(rates.blocked("192.0.2.1", connection, ConnectionRates.Kind.STATUS, now)).isFalse();
            assertThat(rates.blocked("192.0.2.1", connection, ConnectionRates.Kind.CHECK, now)).isFalse();
            assertThat(rates.blocked("192.0.2.1", connection, ConnectionRates.Kind.STATUS, now)).isFalse();
        }
        assertThat(rates.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.STATUS, now)).isTrue();
        for (int i = 0; i < 60; i++) {
            Object connection = new Object();
            assertThat(rates.blocked("192.0.2.2", connection, ConnectionRates.Kind.LOGIN, now)).isFalse();
            assertThat(rates.blocked("192.0.2.2", connection, ConnectionRates.Kind.CHECK, now)).isFalse();
        }
        assertThat(rates.blocked("192.0.2.2", new Object(), ConnectionRates.Kind.LOGIN, now)).isTrue();
    }
    @Test void expiryPersistenceAndUnban() {
        var rates = rates();
        for (int i = 0; i < 61; i++) rates.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.LOGIN, now);
        rates.save();
        var restored = rates(); restored.load(now);
        assertThat(restored.list(now).get("192.0.2.1").reason()).isEqualTo("login rate");
        assertThat(restored.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.CHECK, now)).isTrue();
        assertThat(restored.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.LOGIN, now + ConnectionRates.BAN_TIME)).isFalse();
        assertThat(rates.unban("192.0.2.1")).isTrue(); rates.save();
        var unbanned = rates(); unbanned.load(now);
        assertThat(unbanned.list(now)).isEmpty();
        assertThat(rates.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.LOGIN, now)).isFalse();
    }
    @Test void independentIpsAndWindowExpiry() {
        var rates = rates();
        for (int i = 0; i < 60; i++) rates.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.LOGIN, now);
        assertThat(rates.blocked("192.0.2.2", new Object(), ConnectionRates.Kind.LOGIN, now)).isFalse();
        assertThat(rates.blocked("192.0.2.1", new Object(), ConnectionRates.Kind.LOGIN, now + ConnectionRates.WINDOW)).isFalse();
    }
}
