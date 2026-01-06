package com.skycatdev.antiscan.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.api.ConnectionCheckerType;
import com.skycatdev.antiscan.api.VerificationStatus;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HunterChecker extends VerificationList {
    public static final long DEFAULT_UPDATE_DELAY = 10800L;
    public static final long DEFAULT_LAST_UPDATED = 0L;
    public static final MapCodec<HunterChecker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("list").forGetter(VerificationList::exportList),
            Codec.LONG.fieldOf("last_updated").orElse(DEFAULT_LAST_UPDATED).forGetter(HunterChecker::getLastUpdated),
            Codec.LONG.fieldOf("update_delay_sec").orElse(DEFAULT_UPDATE_DELAY).forGetter(HunterChecker::getUpdateDelay) // Magic 10800L - 3 hours
    ).apply(instance, HunterChecker::new));
    public static final String HUNTER_URL = "https://raw.githubusercontent.com/pebblehost/hunter/refs/heads/master/ips.txt";
    protected final Object lastUpdatedLock = new Object[]{};
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
        super(list, true, true);
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
        synchronized (lastUpdatedLock) {
            return lastUpdated;
        }
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
    protected boolean updateIfNeeded() {
        synchronized (lastUpdatedLock) {
            if (System.currentTimeMillis() > lastUpdated + updateDelay * 100) { // Magic 100: ms in 1 sec
                return updateNow();
            }
        }
        return true;
    }

    protected boolean updateNow() {
        DataResult<HttpResponse<String>> request = Utils.httpRequest(HttpRequest.newBuilder()
                .uri(URI.create(HUNTER_URL))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        // https://curlconverter.com/java/
        if (request.isSuccess()) {
            HttpResponse<String> response = request.getOrThrow();
            clear();
            // There is a brief time here where the list is empty. I don't think it really matters that much.
            addAll(Arrays.asList(response.body().split("\n")));
            synchronized (lastUpdatedLock) {
                lastUpdated = System.currentTimeMillis();
            }
            Antiscan.LOGGER.info("Updated IP blacklist from Hunter.");
            return true;
        } else {
            if (request.hasResultOrPartial()) {
                HttpResponse<String> response = request.getPartialOrThrow();
                Antiscan.LOGGER.warn("Failed to load ip blacklist from hunter - got a non-2xx status code: {}. Response: {}", response.statusCode(), response.body());
            } else {
                Antiscan.LOGGER.warn("Failed to load ip blacklist from hunter. This is NOT a fatal error. Reason: {}", request.error().orElseThrow().message());
            }
            return false;
        }
    }
}
