package com.skycatdev.antiscan.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.fabricmc.loader.api.FabricLoader;

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
        AbuseIpdbChecker abuseIpdbChecker
) {
    public static final String DEFAULT_COMMENT = "Hey! Be careful sharing this! It's full of IPs, which may include yours or your friends'.";
    public static final int CONFIG_VERSION = 2;
    public static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("antiscan2.json").toFile();
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("_read_this").forGetter((ignored) -> DEFAULT_COMMENT),
            Codec.INT.fieldOf("config_version_dont_touch").forGetter((ignored) -> CONFIG_VERSION),
            VerificationList.CODEC.codec().fieldOf("ip_whitelist").forGetter(Config::ipWhitelist),
            VerificationList.CODEC.codec().fieldOf("ip_blacklist").forGetter(Config::ipBlacklist),
            VerificationList.CODEC.codec().fieldOf("name_whitelist").forGetter(Config::nameWhitelist),
            VerificationList.CODEC.codec().fieldOf("name_blacklist").forGetter(Config::nameBlacklist),
            HunterChecker.CODEC.codec().fieldOf("hunter").forGetter(Config::hunterChecker),
            AbuseIpdbChecker.CODEC.codec().fieldOf("abuse_ipdb").forGetter(Config::abuseIpdbChecker)
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
                new AbuseIpdbChecker()
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
