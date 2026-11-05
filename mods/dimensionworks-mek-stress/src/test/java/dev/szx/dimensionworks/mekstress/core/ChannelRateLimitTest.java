package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChannelRateLimitTest {
    @Test void capsAtOneSecondAndCarriesFractionalItems() {
        var limit = new ChannelRateLimit(50);
        limit.tick(1, true);
        assertEquals(2, limit.available());
        limit.commit(2);
        limit.tick(2, true);
        assertEquals(3, limit.available());
        limit.commit(3);
        for (int tick = 3; tick < 100; tick++) limit.tick(tick, true);
        assertEquals(50, limit.available());
        limit.tick(99, true);
        assertEquals(50, limit.available());
    }

    @Test void unloadedOrSkippedTicksDoNotEarnOfflineCredit() {
        var limit = new ChannelRateLimit(50_000);
        limit.tick(1, true);
        limit.commit(2500);
        limit.tick(2, false);
        limit.tick(10000, true);
        assertEquals(2500, limit.available());
        assertThrows(IllegalArgumentException.class, () -> limit.commit(2501));
    }
}
