package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicOutputPlannerTest {
    @Test
    void requiresEveryOutputToFit() {
        assertTrue(AtomicOutputPlanner.canFit(16, 16, 64));
        assertTrue(AtomicOutputPlanner.allFit(32, 64));
        assertFalse(AtomicOutputPlanner.allFit(64, 32));
    }

    @Test
    void exactCapacityFits() {
        assertTrue(AtomicOutputPlanner.canFit(64, 0, 64));
        assertFalse(AtomicOutputPlanner.canFit(65, 0, 64));
    }

    @Test
    void clampsNegativeValues() {
        assertTrue(AtomicOutputPlanner.canFit(-5, 0, 64));
    }

    @Test
    void outputPlannerHonorsRealStackAndSlotCapacity() {
        var oneSlot = List.of(new AtomicOutputPlanner.SlotState(null, 0, 64, 64));
        var twoSlots = List.of(
            new AtomicOutputPlanner.SlotState(null, 0, 64, 64),
            new AtomicOutputPlanner.SlotState(null, 0, 64, 64)
        );
        var output = List.of(new AtomicOutputPlanner.OutputState("stone", 65, 64));
        assertFalse(AtomicOutputPlanner.canFitStacks(oneSlot, output));
        assertTrue(AtomicOutputPlanner.canFitStacks(twoSlots, output));
    }

    @Test
    void mergesCompatibleStacksBeforeUsingEmptySlots() {
        var slots = List.of(
            new AtomicOutputPlanner.SlotState("stone", 60, 64, 64),
            new AtomicOutputPlanner.SlotState(null, 0, 64, 64)
        );
        var output = List.of(new AtomicOutputPlanner.OutputState("stone", 8, 64));
        assertTrue(AtomicOutputPlanner.canFitStacks(slots, output));
    }

    @Test
    void rejectsOutputThatExceedsPerSlotLimit() {
        var slots = List.of(
            new AtomicOutputPlanner.SlotState(null, 0, 32, 64),
            new AtomicOutputPlanner.SlotState(null, 0, 32, 64)
        );
        var output = List.of(new AtomicOutputPlanner.OutputState("stone", 65, 64));
        assertFalse(AtomicOutputPlanner.canFitStacks(slots, output));
    }
}
