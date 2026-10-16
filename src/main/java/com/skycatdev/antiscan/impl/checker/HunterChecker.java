package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.FeedLimits;
import com.skycatdev.antiscan.impl.Utils;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class HunterChecker extends VerificationList {
    /**
     * Seconds
     */
    public static final long DEFAULT_UPDATE_DELAY = TimeUnit.HOURS.toSeconds(3);
    public static final long DEFAULT_LAST_UPDATED = 0L;
    public static final MapCodec<HunterChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("list").forGetter(VerificationList::exportList),
            Codec.LONG.optionalFieldOf("last_updated", DEFAULT_LAST_UPDATED).forGetter(HunterChecker::getLastUpdated),
            Codec.LONG.fieldOf("update_delay_sec").orElse(DEFAULT_UPDATE_DELAY).forGetter(HunterChecker::getUpdateDelay)
    ).apply(instance, HunterChecker::new));
    public static final String HUNTER_URL = "https://raw.githubusercontent.com/pebblehost/hunter/refs/heads/master/ips.txt";
    /**
     * The minimum number of seconds between each update.
     */
    private final long updateDelay;
    /**
     * The millisecond (since epoch) when the list was updated
     */
    private volatile long lastUpdated;
    private long nextAttempt;

    public HunterChecker() {
        this(new HashSet<>(), DEFAULT_LAST_UPDATED, DEFAULT_UPDATE_DELAY);
    }

    public HunterChecker(List<String> list, long lastUpdated, long updateDelay) {
        this(new HashSet<>(list), lastUpdated, updateDelay);
    }

    public HunterChecker(HashSet<String> list, long lastUpdated, long updateDelay) {
        super(list, VerificationStatus.FAIL, true);
        this.lastUpdated = lastUpdated;
        this.updateDelay = updateDelay;
    }

    @Override
    public CompletableFuture<VerificationStatus> check(Connection connection, @Nullable String playerName, Executor executor) {
        if (!FeedLimits.fresh(lastUpdated, System.currentTimeMillis())) return CompletableFuture.completedFuture(VerificationStatus.PASS);
        return super.check(connection, playerName, executor);
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.HUNTER;
    }

    private long getUpdateDelay() {
        return updateDelay;
    }

    /**
     *
     * @return Success
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean updateIfNeeded() {
        long now = System.currentTimeMillis();
        if (now < nextAttempt || now < lastUpdated + TimeUnit.SECONDS.toMillis(Math.max(60, updateDelay))) return true;
        nextAttempt = now + TimeUnit.MINUTES.toMillis(15);
        return updateNow();
    }

    protected boolean updateNow() {
        var request = Utils.sendHttpRequest(HttpRequest.newBuilder(URI.create(HUNTER_URL))
                .timeout(java.time.Duration.ofSeconds(10)).GET().build(), Utils.boundedBody());
        if (!request.isSuccess()) {
            Antiscan.LOGGER.warn("Hunter list download failed ({}); keeping the last good list.",
                    request.resultOrPartial().map(r -> "HTTP " + r.statusCode()).orElse("no response"));
            return false;
        }
        try {
            var replacement = FeedLimits.parseIps(request.getOrThrow().body());
            replaceAll(replacement);
            lastUpdated = System.currentTimeMillis();
            Antiscan.LOGGER.info("Updated Hunter IP list ({} IPs).", replacement.size());
            return true;
        } catch (RuntimeException e) {
            Antiscan.LOGGER.warn("Malformed Hunter feed; retaining last good data");
            return false;
        }
    }
}
