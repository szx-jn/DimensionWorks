package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void configuredRateUsesRepairedPolicyAndNeverRaisesIt() {
        assertEquals(8.0D, MekProcessingGate.configuredRateLimit(
            0, GearHeartQuotaType.MEK_ORE_OPERATIONS, 8, 64));
        assertEquals(64.0D, MekProcessingGate.configuredRateLimit(
            GearHeartStateBits.MEK_ORE_OPERATIONS,
            GearHeartQuotaType.MEK_ORE_OPERATIONS, 8, 64));
        assertEquals(4096.0D, MekProcessingGate.configuredRateLimit(
            0, GearHeartQuotaType.CREATE_CAPACITY, 8, 64));
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

    private static final class GearHeartStateBits {
        private static final int MEK_ORE_OPERATIONS = 1 << 2;
        private static final int ACTIVE_MACHINES = 1 << 5;
    }
}
