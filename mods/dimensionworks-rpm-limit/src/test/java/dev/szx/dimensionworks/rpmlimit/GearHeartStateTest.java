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
        assertEquals(0b0101010, GearHeartState.normalizeMask(0b10101010));
    }

    @Test
    void everyRepairUsesItsSpecifiedRotationLimit() {
        int[] expected = {32, 512, 1024, 2048, 4096, 5120, 8192, 10240};
        for (int stage = 0; stage < expected.length; stage++) {
            int mask = (1 << stage) - 1;
            assertEquals(stage, GearHeartState.repairStage(mask));
            assertEquals(expected[stage], GearHeartState.rotationLimit(mask, 32, 10240));
        }
    }

    @Test
    void missingEarlierRepairsCannotUnlockLaterRotationStages() {
        assertEquals(0, GearHeartState.repairStage(0b1111110));
        assertEquals(2, GearHeartState.repairStage(0b1111011));
        assertEquals(1024, GearHeartState.rotationLimit(0b1111011, 32, 10240));
        assertEquals(7, GearHeartState.repairStage(0b11111111));
    }

    @Test
    void rotationStagesRespectConfiguredBoundsWithoutRescaling() {
        assertEquals(32, GearHeartState.rotationLimit(0, 32, 320));
        assertEquals(320, GearHeartState.rotationLimit(0b111, 32, 320));
        assertEquals(1024, GearHeartState.rotationLimit(0b11, 32, 4096));
        assertEquals(4096, GearHeartState.rotationLimit(0b11111, 32, 4096));
        assertEquals(320, GearHeartState.rotationLimit(GearHeartState.ALL_CURSES, 32, 320));
        assertEquals(100, GearHeartState.rotationLimit(GearHeartState.ALL_CURSES, 100, 80));
    }

    @Test
    void configOnlyContainsTheSevenIndustrialQuotaFamilies() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitConfig.java"));

        assertTrue(source.contains("rotationCursedRpm"));
        assertTrue(source.contains("rotationReleasedRpm"));
        assertTrue(source.contains("createCapacityCursedSu"));
        assertTrue(source.contains("mekOreOperationsCursedPerSecond"));
        assertFalse(source.contains("mekOreOperationsRepairedPerSecond"));
        assertTrue(source.contains("aeCraftingJobsCursed"));
        assertFalse(source.contains("aeCraftingJobsRepaired"));
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
