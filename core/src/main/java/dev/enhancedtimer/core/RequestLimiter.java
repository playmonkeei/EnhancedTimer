package dev.enhancedtimer.core;

import java.util.*;
import java.util.function.LongSupplier;

/** Allows short ordered UI sequences while bounding sustained network traffic. */
public final class RequestLimiter {
    private record Bucket(double tokens, long at) { }
    private final Map<UUID, Bucket> buckets = new HashMap<>();
    private final LongSupplier clock;
    public RequestLimiter() { this(System::nanoTime); }
    RequestLimiter(LongSupplier clock) { this.clock = clock; }
    public synchronized boolean allow(UUID id) {
        long now = clock.getAsLong();
        var before = buckets.getOrDefault(id, new Bucket(10, now));
        double available = Math.min(10, before.tokens() + Math.max(0, now - before.at()) / 50_000_000.0);
        boolean allowed = available >= 1;
        buckets.put(id, new Bucket(allowed ? available - 1 : available, now));
        return allowed;
    }
    public synchronized void retain(Set<UUID> online) { buckets.keySet().retainAll(online); }
}
