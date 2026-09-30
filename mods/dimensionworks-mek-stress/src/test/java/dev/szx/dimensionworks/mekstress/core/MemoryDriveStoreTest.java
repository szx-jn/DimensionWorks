package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryDriveStoreTest {

    @Test
    void truncatesStoredStressWhenCapacityShrinks() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR1);
        store.setConfiguration(2, 16_384L, 8_192L);
        assertEquals(16_384L, store.insert(16_384L));
        assertEquals(16_384L, store.storedSu());
        store.setConfiguration(1, 8_192L, 8_192L);
        assertEquals(8_192L, store.storedSu());
    }

    @Test
    void neverExceedsTheConfiguredCapacity() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR5);
        store.setConfiguration(4, 65_536L, 131_072L);
        assertEquals(65_536L, store.insert(100_000L));
        assertEquals(65_536L, store.storedSu());
        assertEquals(0L, store.insert(1L));
    }

    @Test
    void storesTheCalculatedMixedDriveBandwidth() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR5);
        store.setConfiguration(64, 1_048_576L, 524_288L);
        assertEquals(524_288L, store.bandwidthRpm());
    }
}
