package dev.szx.dimensionworks.mekstress.integration.mek;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekanism.api.math.FloatingLong;
import org.junit.jupiter.api.Test;

class RecipeEnergyOperationsTest {
    @Test
    void countsExactlyFundedFractionalOperations() {
        FloatingLong cost = FloatingLong.parseFloatingLong("50.1234");
        assertEquals(1, RecipeEnergyOperations.divide(cost, cost));
        assertEquals(3, RecipeEnergyOperations.divide(FloatingLong.parseFloatingLong("150.3702"), cost));
        assertEquals(2, RecipeEnergyOperations.divide(FloatingLong.parseFloatingLong("150.3701"), cost));
    }

    @Test
    void preservesInsufficientAndWholeEnergyResults() {
        assertEquals(0, RecipeEnergyOperations.divide(FloatingLong.parseFloatingLong("50.1233"),
            FloatingLong.parseFloatingLong("50.1234")));
        assertEquals(3, RecipeEnergyOperations.divide(FloatingLong.create(150), FloatingLong.create(50)));
    }
}
