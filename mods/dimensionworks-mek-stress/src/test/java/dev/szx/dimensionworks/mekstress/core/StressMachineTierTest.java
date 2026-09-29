package dev.szx.dimensionworks.mekstress.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressMachineTierTest {

    @Test
    void usesFourIncreasingMechanicalTiers() {
        assertEquals(128, StressMachineTier.BASIC.requiredRpm());
        assertEquals(512, StressMachineTier.ADVANCED.requiredRpm());
        assertEquals(2048, StressMachineTier.ELITE.requiredRpm());
        assertEquals(10240, StressMachineTier.ULTIMATE.requiredRpm());
    }

    @Test
    void unknownMachinesUseTheBasicTier() {
        assertEquals(StressMachineTier.BASIC, StressMachineTier.forBaseTier(null));
        assertEquals(StressMachineTier.ULTIMATE, StressMachineTier.forBaseTier("ULTIMATE"));
        assertEquals(StressMachineTier.ELITE, StressMachineTier.forBaseTier("elite"));
        assertEquals(StressMachineTier.BASIC, StressMachineTier.forBaseTier("CREATIVE"));
    }
}
