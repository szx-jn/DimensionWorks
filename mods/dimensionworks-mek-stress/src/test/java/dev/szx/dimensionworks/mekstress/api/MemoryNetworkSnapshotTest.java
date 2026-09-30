package dev.szx.dimensionworks.mekstress.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryNetworkSnapshotTest {

    @Test
    void emptySnapshotHasNoAvailableMechanicalPower() {
        assertEquals(0.0D, MemoryNetworkSnapshot.empty(MemoryNetworkStatus.NO_DRIVE).finalQ(), 1.0E-9D);
    }
}
