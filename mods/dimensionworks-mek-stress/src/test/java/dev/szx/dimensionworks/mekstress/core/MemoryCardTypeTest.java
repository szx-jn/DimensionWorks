package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MemoryCardTypeTest {

    @Test
    void definesTheSymmetricArchetypeExponents() {
        assertExponents(MemoryCardType.DEFECTIVE, -2, -2);
        assertExponents(MemoryCardType.ECONOMY, -1, -1);
        assertExponents(MemoryCardType.BALANCED, 0, 0);
        assertExponents(MemoryCardType.HIGH_SPEED, -1, 1);
        assertExponents(MemoryCardType.HIGH_STORAGE, 1, -1);
        assertExponents(MemoryCardType.FINAL, 2, 2);
    }

    @Test
    void assignsInputStressReductionContributions() {
        assertEquals(2_000, MemoryCardType.DEFECTIVE.inputReductionBasisPoints());
        assertEquals(200, MemoryCardType.ECONOMY.inputReductionBasisPoints());
        assertEquals(150, MemoryCardType.BALANCED.inputReductionBasisPoints());
        assertEquals(175, MemoryCardType.HIGH_SPEED.inputReductionBasisPoints());
        assertEquals(75, MemoryCardType.HIGH_STORAGE.inputReductionBasisPoints());
        assertEquals(0, MemoryCardType.FINAL.inputReductionBasisPoints());
    }

    @Test
    void marksOnlyTheFourNormalArchetypesCraftable() {
        assertTrue(MemoryCardType.ECONOMY.craftable());
        assertTrue(MemoryCardType.BALANCED.craftable());
        assertTrue(MemoryCardType.HIGH_SPEED.craftable());
        assertTrue(MemoryCardType.HIGH_STORAGE.craftable());
        assertFalse(MemoryCardType.DEFECTIVE.craftable());
        assertFalse(MemoryCardType.FINAL.craftable());
    }

    private static void assertExponents(MemoryCardType type, int storageExponent, int speedExponent) {
        assertEquals(speedExponent, type.defaultSpeedExponent());
    }
}
