package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AeCraftingGateTest {

    @Test
    void configuredJobLimitUsesPolicyAndConfiguredValues() {
        assertEquals(1, AeCraftingGate.configuredJobLimit(0, 1, 80));
        assertEquals(80, AeCraftingGate.configuredJobLimit(
            GearHeartStateBits.AE_CRAFTING_JOBS, 1, 80));
        assertEquals(8, AeCraftingGate.configuredActiveLimit(0, 8, 64));
        assertEquals(64, AeCraftingGate.configuredActiveLimit(
            GearHeartStateBits.ACTIVE_MACHINES, 8, 64));
    }

    @Test
    void finalOutputRateIsReleasedAfterSeventhRepair() {
        assertEquals(16.0D, AeCraftingGate.configuredFinalRate(0, 16, 128));
        assertEquals(128.0D, AeCraftingGate.configuredFinalRate(
            GearHeartStateBits.FINAL_PRODUCTION, 16, 128));
        assertTrue(Double.isInfinite(AeCraftingGate.configuredFinalRate(
            GearHeartStateBits.ALL, 16, 128)));
    }

    private static final class GearHeartStateBits {
        private static final int AE_CRAFTING_JOBS = 1 << 3;
        private static final int ACTIVE_MACHINES = 1 << 5;
        private static final int FINAL_PRODUCTION = 1 << 6;
        private static final int ALL = 0b1111111;
    }
}
