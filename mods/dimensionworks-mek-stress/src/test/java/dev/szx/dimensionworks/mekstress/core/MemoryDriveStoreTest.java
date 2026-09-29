package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryDriveStoreTest {

    @Test
    void truncatesStoredStressWhenCapacityShrinks() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR1, 2);
        assertEquals(16_384L, store.insert(16_384L));
        assertEquals(16_384L, store.storedSu());
        store.setCardCount(1);
        assertEquals(8_192L, store.storedSu());
    }

    @Test
    void neverExceedsTheCardCapacity() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR5, 4);
        assertEquals(65_536L, store.insert(100_000L));
        assertEquals(65_536L, store.storedSu());
        assertEquals(0L, store.insert(1L));
    }

    @Test
    void supportsEveryDdrSlotCount() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR5, MemoryTier.DDR5.slotsPerDrive());
        assertEquals(64, store.cardCount());
        assertEquals(1_048_576L, store.capacitySu());
    }
}
