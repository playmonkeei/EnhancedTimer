package dev.enhancedtimer.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RequestLimiterTest {
    @Test void preservesConfigureThenStartButLimitsSustainedTraffic() {
        var clock = new AtomicLong(); var limiter = new RequestLimiter(clock::get); var player = UUID.randomUUID();
        assertTrue(limiter.allow(player)); assertTrue(limiter.allow(player));
        for (int i = 0; i < 8; i++) assertTrue(limiter.allow(player));
        assertFalse(limiter.allow(player));
        clock.addAndGet(50_000_000); assertTrue(limiter.allow(player)); assertFalse(limiter.allow(player));
        assertTrue(limiter.allow(UUID.randomUUID()));
        limiter.retain(Set.of()); assertTrue(limiter.allow(player));
    }
}
