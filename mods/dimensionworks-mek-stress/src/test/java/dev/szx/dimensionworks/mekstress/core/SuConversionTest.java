package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SuConversionTest {
    @Test
    void convertsWholeFeAtTwoPointFive() {
        assertEquals(1L, SuConversion.ceilFromFe(2L, 2.5D));
        assertEquals(2L, SuConversion.ceilFromFe(3L, 2.5D));
        assertEquals(2L, SuConversion.ceilFromFe(5L, 2.5D));
    }

    @Test
    void rejectsInvalidRatios() {
        assertThrows(IllegalArgumentException.class, () -> SuConversion.ceilFromFe(1L, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> SuConversion.ceilFromFe(1L, Double.NaN));
    }
}
