package dev.szx.dimensionworks.rpmlimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GearHeartStateTest {

    @Test
    void repairsMustBeStrictlySequential() {
        assertEquals(1, GearHeartState.nextRepairIndex(0));
        assertEquals(3, GearHeartState.nextRepairIndex(0b0000011));
        assertEquals(0, GearHeartState.nextRepairIndex(GearHeartState.ALL_CURSES));

        assertTrue(GearHeartState.canRepair(0, 1));
        assertFalse(GearHeartState.canRepair(0, 2));
        assertTrue(GearHeartState.canRepair(0b0000011, 3));
        assertFalse(GearHeartState.canRepair(0b0000101, 3));
        assertFalse(GearHeartState.canRepair(GearHeartState.ALL_CURSES, 7));
    }

    @Test
    void seventhRepairCompletesTheMask() {
        assertTrue(GearHeartState.isFullyReleasedMask(0b1111111));
        assertFalse(GearHeartState.isFullyReleasedMask(0b0111111));
        assertEquals(0b0101010, GearHeartState.normalizeLegacyMask(0b10101010));
    }

    @Test
    void configOnlyContainsTheSevenIndustrialQuotaFamilies() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitConfig.java"));

        assertTrue(source.contains("rotationCursedRpm"));
        assertTrue(source.contains("rotationRepairedRpm"));
        assertTrue(source.contains("createCapacityCursedSu"));
        assertTrue(source.contains("mekOreOperationsCursedPerSecond"));
        assertTrue(source.contains("mekOreOperationsRepairedPerSecond"));
        assertTrue(source.contains("aeCraftingJobsCursed"));
        assertTrue(source.contains("aeCraftingJobsRepaired"));
        assertTrue(source.contains("crossProcessItemsCursedPerSecond"));
        assertTrue(source.contains("crossProcessItemsRepairedPerSecond"));
        assertTrue(source.contains("activeMachinesCursed"));
        assertTrue(source.contains("activeMachinesRepaired"));
        assertTrue(source.contains("finalProductionItemsCursedPerSecond"));
        assertTrue(source.contains("finalProductionItemsRepairedPerSecond"));

        assertFalse(source.contains("outputFailureChance"));
        assertFalse(source.contains("freezeChance"));
        assertFalse(source.contains("freezeIntervalMinSeconds"));
        assertFalse(source.contains("freezeIntervalMaxSeconds"));
        assertFalse(source.contains("freezeDurationTicks"));
    }
}
