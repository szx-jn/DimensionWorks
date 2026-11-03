package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PowerBudgetTest {
    @Test void repeatedSimulationDoesNotSpendAndExecutionCannotOverdraw() {
        PowerBudget budget = new PowerBudget();
        for (int i = 0; i < 20; i++) assertEquals(1000, budget.extract(40, 1000, 5000, false));
        assertEquals(600, budget.extract(40, 1000, 600, true));
        assertEquals(400, budget.extract(40, 1000, 5000, true));
        assertEquals(0, budget.extract(40, 1000, 1, true));
        assertEquals(1000, budget.available(41, 1000));
        assertEquals(1000, budget.available(4000, 1000));
    }

    @Test void repricingWithinOneTickRetainsAlreadySpentPower() {
        PowerBudget budget = new PowerBudget();
        assertEquals(600, budget.extract(10, 1000, 600, true));
        assertEquals(1400, budget.available(10, 2000));
        assertEquals(0, budget.available(10, 100));
        assertEquals(0, budget.extract(10, 2000, Double.NaN, true));
        assertEquals(0, budget.extract(10, 2000, Double.POSITIVE_INFINITY, true));
    }
}
