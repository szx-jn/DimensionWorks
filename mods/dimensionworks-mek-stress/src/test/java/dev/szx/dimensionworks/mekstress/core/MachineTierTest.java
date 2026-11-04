package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineTierTest {

    @Test
    void followsTheMekanismQualityTable() {
        assertEquals(1_024, MachineTier.BASIC.targetRpm());
        assertEquals(1_024L, MachineTier.BASIC.bandwidthRpm());
        assertEquals(1_024, MachineTier.ULTIMATE.targetRpm());
        assertEquals(8_192L, MachineTier.ULTIMATE.bandwidthRpm());
    }
}
