package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmpouleStateTest {

    private static final String ABYSSAL = "alex_caves_dimensions:abyssal_chasm";
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void departureStartsTimerOnce() {
        AmpouleState fresh = AmpouleState.fresh(ABYSSAL);

        AmpouleState departed = AmpouleState.observeDimension(fresh, OVERWORLD, 1_000L, 60_000L);
        AmpouleState movedAgain = AmpouleState.observeDimension(departed, "minecraft:nether", 20_000L, 60_000L);

        assertEquals(61_000L, departed.expiresAtMillis());
        assertEquals(departed.expiresAtMillis(), movedAgain.expiresAtMillis());
    }

    @Test
    void returningToOriginDoesNotPauseOrClearTimer() {
        AmpouleState departed = AmpouleState.observeDimension(
            AmpouleState.fresh(ABYSSAL), OVERWORLD, 1_000L, 60_000L
        );

        AmpouleState returned = AmpouleState.observeDimension(departed, ABYSSAL, 2_000L, 60_000L);

        assertSame(departed, returned);
    }

    @Test
    void sameFilledStackStateCanMergeAndDifferentTimestampsCannot() {
        AmpouleState first = new AmpouleState(ABYSSAL, 61_000L);
        AmpouleState same = new AmpouleState(ABYSSAL, 61_000L);
        AmpouleState later = new AmpouleState(ABYSSAL, 62_000L);

        assertTrue(first.canMerge(same));
        assertFalse(first.canMerge(later));
        assertFalse(first.canMerge(new AmpouleState(OVERWORLD, 61_000L)));
    }

    @Test
    void expirationUsesInclusiveBoundary() {
        AmpouleState state = new AmpouleState(ABYSSAL, 61_000L);

        assertFalse(state.isExpired(60_999L));
        assertTrue(state.isExpired(61_000L));
    }
}
