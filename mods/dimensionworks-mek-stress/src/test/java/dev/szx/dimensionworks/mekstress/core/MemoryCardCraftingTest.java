package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MemoryCardCraftingTest {

    @Test
    void rollsEachOutputIndependentlyAndConservesTheTotal() {
        double[] rolls = {0.10D, 0.80D, 0.20D, 0.99D};
        AtomicInteger index = new AtomicInteger();
        int defective = MemoryCardCrafting.defectiveCount(4, 0.25D, () -> rolls[index.getAndIncrement()]);

        assertEquals(2, defective);
        assertEquals(4, index.get());
        assertEquals(4, defective + (4 - defective));
        assertTrue(defective <= 4);
    }

    @Test
    void handlesGuaranteedAndImpossibleOutcomes() {
        assertEquals(4, MemoryCardCrafting.defectiveCount(4, 1.0D, () -> 0.99D));
        assertEquals(0, MemoryCardCrafting.defectiveCount(4, 0.0D, () -> 0.0D));
        assertEquals(0, MemoryCardCrafting.defectiveCount(0, 0.25D, () -> 0.0D));
    }
}
