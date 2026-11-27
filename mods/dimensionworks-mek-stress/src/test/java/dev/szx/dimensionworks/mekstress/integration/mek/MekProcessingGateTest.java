package dev.szx.dimensionworks.mekstress.integration.mek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.szx.dimensionworks.rpmlimit.api.IQuotaGateService.QuotaFamily;
import dev.szx.dimensionworks.rpmlimit.api.RpmLimitServices;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MekProcessingGateTest {

    private final UUID owner = UUID.fromString("1159fa7d-13d4-4f74-a711-48a44c8c5252");

    @BeforeEach
    void clearActiveMachines() {
        MekProcessingGate.clearActives();
    }

    @Test
    void activeMachinesAreCountedPerOwnerAndContinuingMachinesReuseTheirSlot() {
        assertTrue(MekProcessingGate.reserveActive(
            new Object(), owner, 0, 100L, 2));
        assertTrue(MekProcessingGate.reserveActive(
            new Object(), owner, 0, 100L, 2));
        assertFalse(MekProcessingGate.reserveActive(
            new Object(), owner, 0, 100L, 2));
        assertEquals(2, MekProcessingGate.activeCount(owner, 100L));
    }

    @Test
    void activeMachineRepairReleasesOnlyThatOwner() {
        Object machine = new Object();
        assertTrue(MekProcessingGate.reserveActive(
            machine, owner, GearHeartStateBits.ACTIVE_MACHINES, 200L, 8));

        UUID other = UUID.fromString("2f7a0388-bbf0-4d17-bdb6-8796e717a260");
        for (int i = 0; i < 8; i++) {
            assertTrue(MekProcessingGate.reserveActive(
                new Object(), other, 0, 200L, 8));
        }
        assertFalse(MekProcessingGate.reserveActive(
            new Object(), other, 0, 200L, 8));
    }

    @Test
    void quotaApiExposesActiveMachineAndOreFlagsWithoutLeakingMekanismTypes() {
        assertTrue(RpmLimitServices.quota().isRepairApplied(
            GearHeartStateBits.ACTIVE_MACHINES, QuotaFamily.ACTIVE_MACHINES));
        assertFalse(RpmLimitServices.quota().isRepairApplied(0, QuotaFamily.MEK_ORE_OPERATIONS));
    }

    private static final class GearHeartStateBits {
        private static final int ACTIVE_MACHINES = 1 << 5;
    }
}
