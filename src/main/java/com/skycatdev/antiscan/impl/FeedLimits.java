package com.skycatdev.antiscan.impl;

import com.google.common.net.InetAddresses;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Limits and parsing shared by the downloaded IP lists. */
public final class FeedLimits {
    public static final long MAX_FEED_AGE = TimeUnit.DAYS.toMillis(7);
    public static final int MAX_FEED_ENTRIES = 100_000;
    public static final int MAX_FEED_BYTES = 8 * 1024 * 1024;

    private FeedLimits() { }

    /** Parses one IP per line, ignoring blank lines and # comments. Throws if the list is empty, oversized or malformed. */
    public static Set<String> parseIps(String body) {
        if (body.length() > MAX_FEED_BYTES) throw new IllegalArgumentException("Feed too large");
        Set<String> ips = new HashSet<>();
        for (var lines = body.lines().iterator(); lines.hasNext();) {
            String ip = lines.next().trim();
            if (ip.isEmpty() || ip.startsWith("#")) continue;
            ips.add(InetAddresses.forString(ip).getHostAddress());
            if (ips.size() > MAX_FEED_ENTRIES) throw new IllegalArgumentException("Too many IPs");
        }
        if (ips.isEmpty()) throw new IllegalArgumentException("Empty IP feed");
        return Set.copyOf(ips);
    }

    /** Downloaded data is only trusted for a week; after that we fall back to not using it. */
    public static boolean fresh(long fetched, long now) { return fetched > 0 && fetched <= now && now - fetched < MAX_FEED_AGE; }
}
