package com.skycatdev.antiscan.impl.checker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import com.skycatdev.antiscan.impl.Utils;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
//? if <=1.20.5
//import java.util.Optional;
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
     * Locks updateDelay, lastUpdated, and updating from Hunter as a whole.
     */
    protected final Object updateLock = new Object[]{};
    /**
     * The minimum number of seconds between each update.
     */
    private long updateDelay;
    /**
     * The millisecond (since epoch) when the list was updated
     */
    private long lastUpdated;

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
        CompletableFuture.runAsync(this::updateIfNeeded, executor);
        return super.check(connection, playerName, executor);
    }

    /**
     * Warning: this is blocking. It will mostly be blocked only while updating.
     */
    public long getLastUpdated() {
        synchronized (updateLock) {
            return lastUpdated;
        }
    }

    @Override
    public ConnectionCheckerType<?> getType() {
        return ConnectionCheckers.HUNTER;
    }

    private long getUpdateDelay() {
        synchronized (updateLock) {
            return updateDelay;
        }
    }

    /**
     *
     * @return Success
     */
    @SuppressWarnings("UnusedReturnValue")
    protected boolean updateIfNeeded() {
        synchronized (updateLock) {
            if (System.currentTimeMillis() > lastUpdated + TimeUnit.SECONDS.toMillis(updateDelay)) {
                return updateNow();
            }
        }
        return true;
    }

    protected boolean updateNow() {
        DataResult<HttpResponse<String>> request = Utils.sendHttpRequest(HttpRequest.newBuilder()
                .uri(URI.create(HUNTER_URL))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        // https://curlconverter.com/java/
        if (request.isSuccess()) {
            HttpResponse<String> response = request.getOrThrow();
            clear();
            // There is a brief time here where the list is empty. I don't think it really matters that much.
            addAll(Arrays.asList(response.body().split("\n")));
            synchronized (updateLock) {
                lastUpdated = System.currentTimeMillis();
            }
            Antiscan.LOGGER.info("Updated IP blacklist from Hunter.");
            return true;
        } else {
            //? if >1.20.5 {
            if (request.hasResultOrPartial()) {
                HttpResponse<String> response = request.getPartialOrThrow();
            //? } else {
            /*Optional<HttpResponse<String>> partial = request.resultOrPartial();
            if (partial.isPresent()) {
                HttpResponse<String> response = partial.get();
            *///? }
                Antiscan.LOGGER.warn("Failed to load ip blacklist from hunter - got a non-2xx status code: {}. Response: {}",
                        response.statusCode(),
                        response.body());
            } else {
                Antiscan.LOGGER.warn("Failed to load ip blacklist from hunter. This is NOT a fatal error. Reason: {}",
                        request.error().orElseThrow().message());
            }
            return false;
        }
    }
}
