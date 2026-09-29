package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineTierTest {

    @Test
    void followsTheMekanismQualityTable() {
        assertEquals(512, MachineTier.BASIC.targetRpm());
        assertEquals(4096L, MachineTier.BASIC.suPerTick());
        assertEquals(512L, MachineTier.BASIC.bandwidthRpm());
        assertEquals(4096, MachineTier.ULTIMATE.targetRpm());
        assertEquals(32768L, MachineTier.ULTIMATE.suPerTick());
        assertEquals(4096L, MachineTier.ULTIMATE.bandwidthRpm());
    }
}
