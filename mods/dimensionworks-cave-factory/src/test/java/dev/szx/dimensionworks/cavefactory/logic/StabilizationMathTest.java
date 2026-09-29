package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StabilizationMathTest {

    @Test
    void fourAmpoulesProduceOneMatrix() {
        StabilizationMath.Result result = StabilizationMath.evaluate(1_000, 1_000);

        assertEquals(1, result.outputs());
        assertEquals(0, result.remainingFluid());
        assertTrue(result.hasOutput());
    }

    @Test
    void retainsPartialFluidUntilEnoughIsAvailable() {
        StabilizationMath.Result result = StabilizationMath.evaluate(750, 1_000);

        assertEquals(0, result.outputs());
        assertEquals(750, result.remainingFluid());
        assertFalse(result.hasOutput());
    }

    @Test
    void clampsMalformedRecipeCharge() {
        StabilizationMath.Result result = StabilizationMath.evaluate(2_000, 0);

        assertEquals(2, result.outputs());
        assertEquals(0, result.remainingFluid());
    }
}
