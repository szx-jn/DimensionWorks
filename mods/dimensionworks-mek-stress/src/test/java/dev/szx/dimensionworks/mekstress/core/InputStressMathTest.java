package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InputStressMathTest {

    @Test
    void capsTheNetworkWideReductionAtNinetyNinePercent() {
        assertEquals(9_900, InputStressMath.cappedReductionBasisPoints(9_900));
        assertEquals(9_900, InputStressMath.cappedReductionBasisPoints(10_000));
        assertEquals(9_900, InputStressMath.cappedReductionBasisPoints(Long.MAX_VALUE));
        assertEquals(9_899, InputStressMath.cappedReductionBasisPoints(9_899));
        assertEquals(0, InputStressMath.cappedReductionBasisPoints(-50));
    }

    @Test
    void excessiveLossRetainsOnePercentWithoutOverflow() {
        InputStressMath.Conversion conversion =
            InputStressMath.convert(Long.MAX_VALUE, 10_000, 0L);
        assertEquals(Long.MAX_VALUE / 100, conversion.effectiveSu());
        assertEquals(700L, conversion.remainderNumerator());
    }

    @Test
    void tinyTransfersAtMaximumLossCarryTheirFractionalRemainder() {
        long remainder = 0;
        long effective = 0;
        for (int i = 0; i < 100; i++) {
            InputStressMath.Conversion conversion = InputStressMath.convert(1L, 9_900, remainder);
            effective += conversion.effectiveSu();
            remainder = conversion.remainderNumerator();
        }
        assertEquals(1L, effective);
        assertEquals(0L, remainder);
    }

    @Test
    void splitInputTransfersMatchTheAggregateDiscount() {
        InputStressMath.Conversion first = InputStressMath.convert(25L, 150, 0L);
        InputStressMath.Conversion second = InputStressMath.convert(25L, 150, first.remainderNumerator());
        InputStressMath.Conversion third = InputStressMath.convert(25L, 150, second.remainderNumerator());
        InputStressMath.Conversion fourth = InputStressMath.convert(25L, 150, third.remainderNumerator());

        assertEquals(98L, first.effectiveSu() + second.effectiveSu()
            + third.effectiveSu() + fourth.effectiveSu());
        assertEquals(5_000L, fourth.remainderNumerator());
    }

    @Test
    void conversionDoesNotOverflowForMaximumSu() {
        InputStressMath.Conversion conversion =
            InputStressMath.convert(Long.MAX_VALUE, 0, 0L);
        assertEquals(Long.MAX_VALUE, conversion.effectiveSu());
        assertEquals(0L, conversion.remainderNumerator());
    }
}
