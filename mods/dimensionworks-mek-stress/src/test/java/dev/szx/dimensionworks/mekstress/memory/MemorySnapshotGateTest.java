package dev.szx.dimensionworks.mekstress.memory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MemorySnapshotGateTest {
    @Test
    void reusesOnlyAnUnchangedSnapshotFromTheSameTick() {
        MemorySnapshotGate gate = new MemorySnapshotGate();
        assertTrue(gate.needsRefresh(20));
        gate.published(20);
        assertFalse(gate.needsRefresh(20));
        assertTrue(gate.needsRefresh(21));
        gate.published(21);
        assertFalse(gate.needsRefresh(21));
    }

    @Test
    void inputMutationForcesRefreshEvenWithinTheSameTick() {
        MemorySnapshotGate gate = new MemorySnapshotGate();
        gate.published(20);
        gate.invalidate();
        assertTrue(gate.needsRefresh(20));
        gate.published(20);
        assertFalse(gate.needsRefresh(20));
    }
}
