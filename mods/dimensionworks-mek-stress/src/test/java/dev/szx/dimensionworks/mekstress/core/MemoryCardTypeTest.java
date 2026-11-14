package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MemoryCardTypeTest {

    @Test
    void definesTheSixRpmBandwidthExponents() {
        assertExponent(MemoryCardType.DEFECTIVE, -2);
        assertExponent(MemoryCardType.ECONOMY, -1);
        assertExponent(MemoryCardType.BALANCED, 0);
        assertExponent(MemoryCardType.HIGH_SPEED, 1);
        assertExponent(MemoryCardType.LOW_LOSS, -1);
        assertExponent(MemoryCardType.FINAL, 2);
    }

    @Test
    void assignsInputStressReductionContributions() {
        assertEquals(2_000, MemoryCardType.DEFECTIVE.inputReductionBasisPoints());
        assertEquals(200, MemoryCardType.ECONOMY.inputReductionBasisPoints());
        assertEquals(150, MemoryCardType.BALANCED.inputReductionBasisPoints());
        assertEquals(175, MemoryCardType.HIGH_SPEED.inputReductionBasisPoints());
        assertEquals(75, MemoryCardType.LOW_LOSS.inputReductionBasisPoints());
        assertEquals(0, MemoryCardType.FINAL.inputReductionBasisPoints());
    }

    @Test
    void marksOnlyTheFourNormalArchetypesCraftable() {
        assertTrue(MemoryCardType.ECONOMY.craftable());
        assertTrue(MemoryCardType.BALANCED.craftable());
        assertTrue(MemoryCardType.HIGH_SPEED.craftable());
        assertTrue(MemoryCardType.LOW_LOSS.craftable());
        assertFalse(MemoryCardType.DEFECTIVE.craftable());
        assertFalse(MemoryCardType.FINAL.craftable());
    }

    private static void assertExponent(MemoryCardType type, int speedExponent) {
        assertEquals(speedExponent, type.defaultSpeedExponent());
    }
}
