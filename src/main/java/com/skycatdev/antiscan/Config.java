package com.skycatdev.antiscan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Config {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("abuseIpdbKey").forGetter(config -> Optional.ofNullable(config.getAbuseIpdbKey())),
            IpMode.CODEC.fieldOf("handshakeMode").forGetter(Config::getHandshakeMode),
            Action.CODEC.fieldOf("handshakeAction").forGetter(Config::getHandshakeAction),
            Codec.BOOL.fieldOf("handshakeReport").forGetter(Config::isHandshakeReport),
            NameIpMode.CODEC.fieldOf("loginMode").forGetter(Config::getLoginMode),
            Action.CODEC.fieldOf("loginAction").forGetter(Config::getLoginAction),
            Codec.BOOL.fieldOf("loginReport").forGetter(Config::isLoginReport),
            IpMode.CODEC.fieldOf("queryMode").forGetter(Config::getQueryMode),
            Action.CODEC.fieldOf("queryAction").forGetter(Config::getQueryAction),
            Codec.BOOL.fieldOf("queryReport").forGetter(Config::isQueryReport),
            IpMode.CODEC.fieldOf("pingMode").forGetter(Config::getPingMode),
            Action.CODEC.fieldOf("pingAction").forGetter(Config::getPingAction),
            Codec.BOOL.fieldOf("pingReport").forGetter(Config::isPingReport),
            Codec.LONG.optionalFieldOf("blacklistUpdateCooldown", TimeUnit.HOURS.toMillis(5)).forGetter(Config::getBlacklistUpdateCooldown)
    ).apply(instance, Config::new));
    // Update config options ONLY on the server thread
    protected @Nullable String abuseIpdbKey;
    protected IpMode handshakeMode;
    protected Action handshakeAction;
    protected boolean handshakeReport;
    protected NameIpMode loginMode;
    protected Action loginAction;
    protected boolean loginReport;
    protected IpMode queryMode;
    protected Action queryAction;
    protected boolean queryReport;
    protected IpMode pingMode;
    protected Action pingAction;
    protected boolean pingReport;
    protected long blacklistUpdateCooldown;

    public Config(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> abuseIpdbKey,
                  IpMode handshakeMode,
                  Action handshakeAction,
                  boolean handshakeReport,
                  NameIpMode loginMode,
                  Action loginAction,
                  boolean loginReport,
                  IpMode queryMode,
                  Action queryAction,
                  boolean queryReport,
                  IpMode pingMode,
                  Action pingAction,
                  boolean pingReport,
                  long blacklistUpdateCooldown) {
        this(abuseIpdbKey.orElse(null),
                handshakeMode,
                handshakeAction,
                handshakeReport,
                loginMode,
                loginAction,
                loginReport,
                queryMode,
                queryAction,
                queryReport,
                pingMode,
                pingAction,
                pingReport,
                blacklistUpdateCooldown);
    }

    public Config(@Nullable String abuseIpdbKey,
                  IpMode handshakeMode,
                  Action handshakeAction,
                  boolean handshakeReport,
                  NameIpMode loginMode,
                  Action loginAction,
                  boolean loginReport,
                  IpMode queryMode,
                  Action queryAction,
                  boolean queryReport,
                  IpMode pingMode,
                  Action pingAction,
                  boolean pingReport,
                  long blacklistUpdateCooldown) {
        this.abuseIpdbKey = abuseIpdbKey;
        this.handshakeMode = handshakeMode;
        this.handshakeAction = handshakeAction;
        this.handshakeReport = handshakeReport;
        this.loginMode = loginMode;
        this.loginAction = loginAction;
        this.loginReport = loginReport;
        this.queryMode = queryMode;
        this.queryAction = queryAction;
        this.queryReport = queryReport;
        this.pingMode = pingMode;
        this.pingAction = pingAction;
        this.pingReport = pingReport;
        this.blacklistUpdateCooldown = blacklistUpdateCooldown;
    }

    public long getBlacklistUpdateCooldown() {
        return AntiScan.IS_DEV_MODE ? TimeUnit.SECONDS.toMillis(30) : blacklistUpdateCooldown;
    }

    public void setBlacklistUpdateCooldown(long blacklistUpdateCooldown, @Nullable File saveFile) throws IOException {
        this.blacklistUpdateCooldown = blacklistUpdateCooldown;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public Config() {
        this.abuseIpdbKey = null;
        this.handshakeMode = IpMode.MATCH_NONE;
        this.handshakeAction = Action.NOTHING;
        this.handshakeReport = false;
        this.loginMode = NameIpMode.MATCH_EITHER;
        this.loginAction = Action.TIMEOUT;
        this.loginReport = true;
        this.queryMode = IpMode.MATCH_NONE;
        this.queryAction = Action.DISCONNECT;
        this.queryReport = true;
        this.pingMode = IpMode.MATCH_NONE;
        this.pingAction = Action.DISCONNECT;
        this.pingReport = true;
    }

    public static Config load(File saveFile) throws IOException {
        return Utils.loadFromFile(saveFile, CODEC);
    }

    public static Config loadOrCreate(File saveFile) {
        if (!saveFile.exists()) {
            AntiScan.LOGGER.info("Creating a new ip blacklist.");
            return new Config();
        }
        try {
            return load(saveFile);
        } catch (IOException e) {
            AntiScan.LOGGER.warn("Failed to load ip blacklist from save file. This is NOT a detrimental error.", e);
            return new Config();
        }
    }

    public @Nullable String getAbuseIpdbKey() {
        return abuseIpdbKey;
    }

    public Action getHandshakeAction() {
        return handshakeAction;
    }

    public IpMode getHandshakeMode() {
        return handshakeMode;
    }

    public Action getLoginAction() {
        return loginAction;
    }

    public NameIpMode getLoginMode() {
        return loginMode;
    }

    public Action getPingAction() {
        return pingAction;
    }

    public IpMode getPingMode() {
        return pingMode;
    }

    public Action getQueryAction() {
        return queryAction;
    }

    public IpMode getQueryMode() {
        return queryMode;
    }

    public boolean isHandshakeReport() {
        return handshakeReport;
    }

    public boolean isLoginReport() {
        return loginReport;
    }

    public boolean isPingReport() {
        return pingReport;
    }

    public boolean isQueryReport() {
        return queryReport;
    }

    protected void save(File file) throws IOException {
        Utils.saveToFile(this, file, CODEC);
    }

    public void setAbuseIpdbKey(@Nullable String abuseIpdbKey, @Nullable File saveFile) throws IOException {
        this.abuseIpdbKey = abuseIpdbKey;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setHandshakeAction(Action handshakeAction, @Nullable File saveFile) throws IOException {
        this.handshakeAction = handshakeAction;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setHandshakeMode(IpMode handshakeMode, @Nullable File saveFile) throws IOException {
        this.handshakeMode = handshakeMode;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setHandshakeReport(boolean handshakeReport, @Nullable File saveFile) throws IOException {
        this.handshakeReport = handshakeReport;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setLoginAction(Action loginAction, @Nullable File saveFile) throws IOException {
        this.loginAction = loginAction;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setLoginMode(NameIpMode loginMode, @Nullable File saveFile) throws IOException {
        this.loginMode = loginMode;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setLoginReport(boolean loginReport, @Nullable File saveFile) throws IOException {
        this.loginReport = loginReport;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setPingAction(Action pingAction, @Nullable File saveFile) throws IOException {
        this.pingAction = pingAction;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setPingMode(IpMode pingMode, @Nullable File saveFile) throws IOException {
        this.pingMode = pingMode;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setPingReport(boolean pingReport, @Nullable File saveFile) throws IOException {
        this.pingReport = pingReport;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setQueryAction(Action queryAction, @Nullable File saveFile) throws IOException {
        this.queryAction = queryAction;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setQueryMode(IpMode queryMode, @Nullable File saveFile) throws IOException {
        this.queryMode = queryMode;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public void setQueryReport(boolean queryReport, @Nullable File saveFile) throws IOException {
        this.queryReport = queryReport;
        if (saveFile != null) {
            save(saveFile);
        }
    }

    public enum NameIpMode implements StringIdentifiable {
        MATCH_ALL("match_all"),
        MATCH_BOTH("match_both"),
        MATCH_IP("match_ip"),
        MATCH_NAME("match_name"),
        MATCH_EITHER("match_either"),
        MATCH_NONE("match_none");

        public static final Codec<NameIpMode> CODEC = StringIdentifiable.createCodec(NameIpMode::values);
        private final String id;

        NameIpMode(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }

        public static NameIpMode fromId(String id) {
            return switch (id) {
                case "match_all" -> MATCH_ALL;
                case "match_both" -> MATCH_BOTH;
                case "match_ip" -> MATCH_IP;
                case "match_name" -> MATCH_NAME;
                case "match_either" -> MATCH_EITHER;
                case "match_none" -> MATCH_NONE;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }
    }

    public enum IpMode implements StringIdentifiable {
        MATCH_NONE("match_none"),
        MATCH_IP("match_ip"),
        MATCH_ALL("match_all");

        public static final Codec<IpMode> CODEC = StringIdentifiable.createCodec(IpMode::values);
        private final String id;

        IpMode(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }

        public static IpMode fromId(String id) {
            return switch (id) {
                case "match_ip" -> MATCH_IP;
                case "match_all" -> MATCH_ALL;
                case "match_none" -> MATCH_NONE;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }
    }

    public enum Action implements StringIdentifiable {
        NOTHING("nothing"),
        DISCONNECT("disconnect"),
        TIMEOUT("timeout");

        public static final Codec<Action> CODEC = StringIdentifiable.createCodec(Action::values);
        private final String id;

        Action(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }

        public static Action fromId(String id) {
            return switch (id) {
                case "disconnect" -> DISCONNECT;
                case "timeout" -> TIMEOUT;
                case "nothing" -> NOTHING;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }
    }
}