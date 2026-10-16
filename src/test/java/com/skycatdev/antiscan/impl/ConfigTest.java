package com.skycatdev.antiscan.impl;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConfigTest {
    private static final String LISTS = """
            "ip_whitelist":{"list":["192.0.2.1"],"status_on_match":"succeed","is_ip_list":true},
            "ip_blacklist":{"list":["192.0.2.2"],"status_on_match":"fail_report","is_ip_list":true},
            "name_whitelist":{"list":[],"status_on_match":"succeed","is_ip_list":false},
            "name_blacklist":{"list":["blocked"],"status_on_match":"fail_report","is_ip_list":false},
            "hunter":{"list":[],"last_updated":0,"update_delay_sec":123},
            """;

    private static Config parse(String abuseIpdb, String extra) {
        String json = "{\"_read_this\":\"x\",\"config_version_dont_touch\":2," + LISTS + "\"abuse_ipdb\":" + abuseIpdb + extra + "}";
        return Config.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test void unchangedLegacyConfigDecodesAndRetainsSettings() {
        var config = parse("{\"blacklist\":[],\"update_delay_seconds\":456,\"report_cooldown_seconds\":789,\"abuse_confidence_threshold\":95}", "");
        assertThat(config.ipWhitelist().getPartNow(-1).part()).containsExactly("192.0.2.1");
        assertThat(config.nameBlacklist().getPartNow(-1).part()).containsExactly("blocked");
        assertThat(config.abuseIpdbChecker().getReportCooldown()).isEqualTo(789);
        assertThat(config.abuseIpdbChecker().getAbuseConfidenceThreshold()).isEqualTo(95);
        assertThat(config.abuseIpdbChecker().isAutoReport()).isFalse();
        var saved = Config.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow().getAsJsonObject();
        assertThat(saved.getAsJsonObject("hunter").get("update_delay_sec").getAsInt()).isEqualTo(123);
    }

    @Test void autoReportOptionRoundTrips() {
        var config = parse("{\"blacklist\":[],\"auto_report\":true}", "");
        assertThat(config.abuseIpdbChecker().isAutoReport()).isTrue();
        var saved = Config.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow().getAsJsonObject();
        assertThat(saved.getAsJsonObject("abuse_ipdb").get("auto_report").getAsBoolean()).isTrue();
    }
}
