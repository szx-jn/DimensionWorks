package dev.szx.dimensionworks.mekstress.integration.mek;

import mekanism.api.math.FloatingLong;

/** Corrects Mekanism's integer division when the available energy has a fractional part. */
public final class RecipeEnergyOperations {
    private RecipeEnergyOperations() {}

    public static int divide(FloatingLong available, FloatingLong perOperation) {
        int operations = available.divideToInt(perOperation);
        // For divisors greater than one, Mekanism's division drops the dividend's
        // fractional part. This can undercount by one even when energy is exactly sufficient.
        if (perOperation.greaterThan(FloatingLong.ONE) && operations < Integer.MAX_VALUE) {
            FloatingLong nextCost = perOperation.multiply(operations + 1);
            if (!nextCost.equals(FloatingLong.MAX_VALUE) && !available.smallerThan(nextCost)) {
                return operations + 1;
            }
        }
        return operations;
    }
}
