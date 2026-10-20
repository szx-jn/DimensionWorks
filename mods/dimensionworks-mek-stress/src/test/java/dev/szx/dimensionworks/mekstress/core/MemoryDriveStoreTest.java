package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MemoryDriveStoreTest {
    @Test
    void preservesStoredStressWhenCapacityShrinks() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR1);
        store.setConfiguration(4, 32_768L, 8_192L);
        assertEquals(32_768L, store.insert(32_768L));

        store.setConfiguration(1, 8_192L, 2_048L);

        assertEquals(8_192L, store.capacitySu());
        assertEquals(32_768L, store.storedSu());
        assertEquals(0L, store.insert(1L));
        assertEquals(24_576L, store.extract(24_576L));
        assertEquals(8_192L, store.storedSu());
        assertEquals(0L, store.insert(1L));
        assertEquals(8_192L, store.extract(8_192L));
        assertEquals(1L, store.insert(1L));
    }

    @Test
    void restorePreservesOverCapacityReserveForLaterExtraction() {
        MemoryDriveStore store = new MemoryDriveStore(MemoryTier.DDR1);
        store.setConfiguration(1, 8_192L, 2_048L);
        store.restore(10_000L);
        assertEquals(10_000L, store.storedSu());
        assertEquals(0L, store.insert(1L));
    }
}
