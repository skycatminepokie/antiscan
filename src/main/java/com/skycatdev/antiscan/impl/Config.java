package com.skycatdev.antiscan.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.checker.AbuseIpdbChecker;
import com.skycatdev.antiscan.impl.checker.HunterChecker;
import com.skycatdev.antiscan.impl.checker.VerificationList;

import java.io.File;
import java.io.IOException;

public record Config(
        String comment,
        int configVersion,
        VerificationList ipWhitelist,
        VerificationList ipBlacklist,
        VerificationList nameWhitelist,
        VerificationList nameBlacklist,
        HunterChecker hunterChecker,
        AbuseIpdbChecker abuseIpdbChecker,
        Protection protection
) {
    /**
     * Optional blocking features that go beyond the lists. Every field has a default, so older config files still load.
     * Players behind one shared address (CGNAT, a proxy) can turn the rate limits off or raise them.
     */
    public record Protection(boolean rateLimits, int maxConnectionsPerMinute, int maxStatusPerMinute,
                             int maxLoginsPerMinute, boolean networkFeeds) {
        public static final Protection DEFAULT = new Protection(true, 240, 180, 60, true);
        public static final Codec<Protection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("rate_limits_enabled", DEFAULT.rateLimits).forGetter(Protection::rateLimits),
                Codec.intRange(1, 1_000_000).optionalFieldOf("max_connections_per_minute", DEFAULT.maxConnectionsPerMinute).forGetter(Protection::maxConnectionsPerMinute),
                Codec.intRange(1, 1_000_000).optionalFieldOf("max_status_per_minute", DEFAULT.maxStatusPerMinute).forGetter(Protection::maxStatusPerMinute),
                Codec.intRange(1, 1_000_000).optionalFieldOf("max_logins_per_minute", DEFAULT.maxLoginsPerMinute).forGetter(Protection::maxLoginsPerMinute),
                Codec.BOOL.optionalFieldOf("network_feeds_enabled", DEFAULT.networkFeeds).forGetter(Protection::networkFeeds)
        ).apply(instance, Protection::new));
    }

    public static final String DEFAULT_COMMENT = "Hey! Be careful sharing this! It's full of IPs, which may include yours or your friends'.";
    public static final int CONFIG_VERSION = 2;
    // Built from the game dir rather than Antiscan.SAVE_DIRECTORY, since the Antiscan static initializer loads this config.
    public static final File CONFIG_FILE = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("antiscan/antiscan2.json").toFile();
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("_read_this").forGetter((ignored) -> DEFAULT_COMMENT),
            Codec.INT.fieldOf("config_version_dont_touch").forGetter((ignored) -> CONFIG_VERSION),
            VerificationList.CODEC.codec().fieldOf("ip_whitelist").forGetter(Config::ipWhitelist),
            VerificationList.CODEC.codec().fieldOf("ip_blacklist").forGetter(Config::ipBlacklist),
            VerificationList.CODEC.codec().fieldOf("name_whitelist").forGetter(Config::nameWhitelist),
            VerificationList.CODEC.codec().fieldOf("name_blacklist").forGetter(Config::nameBlacklist),
            HunterChecker.CODEC.codec().fieldOf("hunter").forGetter(Config::hunterChecker),
            AbuseIpdbChecker.CODEC.codec().fieldOf("abuse_ipdb").forGetter(Config::abuseIpdbChecker),
            Protection.CODEC.optionalFieldOf("protection", Protection.DEFAULT).forGetter(Config::protection)
    ).apply(instance, Config::new));

    private static Config defaultConfig() {
        return new Config(
                DEFAULT_COMMENT,
                CONFIG_VERSION,
                new VerificationList(VerificationStatus.SUCCEED, true),
                new VerificationList(VerificationStatus.FAIL_REPORT, true),
                new VerificationList(VerificationStatus.SUCCEED, false),
                new VerificationList(VerificationStatus.FAIL_REPORT, false),
                new HunterChecker(),
                new AbuseIpdbChecker(),
                Protection.DEFAULT
        );
    }

    public static Config load() {
        if (CONFIG_FILE.exists()) {
            if (CONFIG_FILE.isFile()) {
                try {
                    return Utils.loadFromFile(CONFIG_FILE, CODEC);
                } catch (IOException e) {
                    Antiscan.LOGGER.error("Failed to read Antiscan config file - perhaps it's corrupted? You could try deleting it, but you'll reset back to default configuration.", e);
                    // I don't want to lose the config without the user knowing. It could max out their AbuseIPDB api key if they aren't careful.
                    throw new RuntimeException(e);
                }
            } else {
                Antiscan.LOGGER.warn("Cannot read Antiscan config file - not a regular file. Is it a folder? Loading default.");
            }
        }
        Config config = defaultConfig();
        config.trySave();
        return config;
    }

    public void trySave() {
        if (!CONFIG_FILE.exists()) {
            try {
                if (!CONFIG_FILE.createNewFile()) {
                    Antiscan.LOGGER.warn("Couldn't save Antiscan config - file couldn't be created.");
                    return;
                }
            } catch (IOException e) {
                Antiscan.LOGGER.warn("Couldn't save Antiscan config - file couldn't be created.", e);
                return;
            }
        }

        if (!CONFIG_FILE.isFile()) {
            Antiscan.LOGGER.warn("Couldn't save Antiscan config - file isn't a regular file.");
            return;
        }

        try {
            Utils.saveToFile(this, CONFIG_FILE, CODEC);
            Antiscan.LOGGER.info("Antiscan config saved.");
        } catch (IOException e) {
            Antiscan.LOGGER.warn("Couldn't save Antiscan config.", e);
        }
    }

}
