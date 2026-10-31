package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LiveStressBudgetTest {
    @Test void removalAndEditingReplaceLiveContributions() {
        var budget = new LiveStressBudget<String>();
        budget.input("a", 1000); budget.input("b", 2000);
        budget.output("motor", 500);
        assertEquals(3000, budget.snapshot(0).rawSu());
        assertEquals(2500, budget.snapshot(0).availableSu());
        budget.input("a", 300); budget.output("motor", 700);
        assertEquals(1600, budget.snapshot(0).availableSu());
        budget.remove("b");
        assertTrue(budget.snapshot(0).overloaded());
        budget.remove("motor");
        assertEquals(300, budget.snapshot(0).availableSu());
    }

    @Test void steadyOutputsNeverDrainAndExactFitIsAllowed() {
        var budget = new LiveStressBudget<String>();
        budget.input("generator", 1000); budget.output("motor", 1000);
        for (int tick = 0; tick < 10000; tick++) {
            assertFalse(budget.snapshot(0).overloaded());
            assertEquals(0, budget.snapshot(0).availableSu());
            assertEquals(1000, budget.snapshot(0).effectiveSu());
        }
        budget.output("second", 1);
        assertTrue(budget.snapshot(0).overloaded());
        budget.remove("generator");
        assertTrue(budget.snapshot(0).overloaded());
        assertEquals(0, budget.snapshot(0).effectiveSu());
    }

    @Test void lossIsAppliedOnceToInputAndNeverPerTick() {
        var budget = new LiveStressBudget<String>();
        budget.input("a", 1000); budget.input("b", 1000); budget.output("load", 1800);
        assertEquals(2000, budget.snapshot(1000).rawSu());
        assertEquals(1800, budget.snapshot(1000).effectiveSu());
        assertFalse(budget.snapshot(1000).overloaded());
        assertEquals(20, budget.snapshot(10000).effectiveSu());
        budget.output("load", 20);
        assertFalse(budget.snapshot(9900).overloaded());
        budget.output("load", 21);
        assertTrue(budget.snapshot(9900).overloaded());
        budget.clear();
        budget.input("a", Long.MAX_VALUE); budget.input("b", 1);
        assertEquals(Long.MAX_VALUE, budget.snapshot(0).rawSu());
        budget.output("max", Long.MAX_VALUE);
        budget.output("extra", 1);
        assertTrue(budget.snapshot(0).overloaded(), "Saturated totals must not hide output overcommit");
        assertEquals(0, budget.snapshot(0).availableSu());
        assertEquals(8192, LiveStressBudget.stress(8, -1024));
        assertEquals(0, LiveStressBudget.stress(Float.NaN, 1024));
    }
}
