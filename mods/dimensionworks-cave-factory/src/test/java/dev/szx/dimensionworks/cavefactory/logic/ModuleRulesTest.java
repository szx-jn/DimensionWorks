package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleRulesTest {

    @Test
    void forbidsPrimordialNumericAndMechanismPair() {
        assertFalse(ModuleRules.isAllowed(
            FactoryModule.BIOMASS_YIELD_AMPLIFIER,
            FactoryModule.BIOMASS_CULTIVATOR
        ));
    }

    @Test
    void allowsCrossDimensionPair() {
        assertTrue(ModuleRules.isAllowed(
            FactoryModule.PRESSURE_BUFFER,
            FactoryModule.POLARITY_INVERTER
        ));
    }

    @Test
    void deterministicBonusTriggersOnExactCounter() {
        assertFalse(DeterministicBonus.triggers(4, 5));
        assertTrue(DeterministicBonus.triggers(5, 5));
        assertTrue(DeterministicBonus.triggers(10, 5));
    }

    @Test
    void deterministicRateUsesLongTermAverage() {
        assertEquals(20, DeterministicBonus.occurrences(100, 20));
        assertEquals(0, DeterministicBonus.occurrences(4, 20));
    }
}
