package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GearHeartQuotaPolicyTest {

    @Test
    void exposesCursedLimits() {
        assertEquals(32.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.ROTATION_SPEED, 0));
        assertEquals(4096.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.CREATE_CAPACITY, 0));
        assertEquals(8.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.MEK_ORE_OPERATIONS, 0));
        assertEquals(1.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.AE_CRAFTING_JOBS, 0));
        assertEquals(16.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.CROSS_PROCESS, 0));
        assertEquals(8.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.ACTIVE_MACHINES, 0));
        assertEquals(16.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.FINAL_PRODUCTION, 0));
    }

    @Test
    void appliesRepairedLimitsPerStage() {
        assertEquals(512.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.ROTATION_SPEED, GearHeartStateBits.ROTATION_SPEED));
        assertTrue(GearHeartQuotaPolicy.isReleased(
            GearHeartQuotaType.CREATE_CAPACITY, GearHeartStateBits.CREATE_CAPACITY));
        assertEquals(64.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.MEK_ORE_OPERATIONS, GearHeartStateBits.MEK_ORE_OPERATIONS));
        assertEquals(80.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.AE_CRAFTING_JOBS, GearHeartStateBits.AE_CRAFTING_JOBS));
        assertEquals(128.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.CROSS_PROCESS, GearHeartStateBits.CROSS_PROCESS));
        assertEquals(64.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.ACTIVE_MACHINES, GearHeartStateBits.ACTIVE_MACHINES));
        assertEquals(128.0, GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.FINAL_PRODUCTION, GearHeartStateBits.FINAL_PRODUCTION));
    }

    @Test
    void finalReleaseRemovesEveryGearHeartQuota() {
        int all = GearHeartStateBits.ALL;
        for (GearHeartQuotaType type : GearHeartQuotaType.values()) {
            assertEquals(Double.POSITIVE_INFINITY, GearHeartQuotaPolicy.limit(type, all));
            assertTrue(GearHeartQuotaPolicy.isReleased(type, all));
        }
    }

    private static final class GearHeartStateBits {
        private static final int ROTATION_SPEED = 1;
        private static final int CREATE_CAPACITY = 1 << 1;
        private static final int MEK_ORE_OPERATIONS = 1 << 2;
        private static final int AE_CRAFTING_JOBS = 1 << 3;
        private static final int CROSS_PROCESS = 1 << 4;
        private static final int ACTIVE_MACHINES = 1 << 5;
        private static final int FINAL_PRODUCTION = 1 << 6;
        private static final int ALL = 0b1111111;
    }
}
