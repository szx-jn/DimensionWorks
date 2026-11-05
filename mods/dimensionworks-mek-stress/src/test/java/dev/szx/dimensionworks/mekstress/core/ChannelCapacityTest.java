package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChannelCapacityTest {
    @Test void capacityIsSharedAndMixedCardsRespectTwentySlots() {
        assertEquals(new ChannelCapacity(27, 16_000), ChannelCapacity.of(0, 0));
        assertEquals(new ChannelCapacity(297, 336_000), ChannelCapacity.of(10, 10));
        assertEquals(new ChannelCapacity(567, 16_000), ChannelCapacity.of(20, 0));
        assertEquals(new ChannelCapacity(27, 656_000), ChannelCapacity.of(0, 20));
        assertThrows(IllegalArgumentException.class, () -> ChannelCapacity.of(1, 20));
    }
}
