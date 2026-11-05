package dev.szx.dimensionworks.mekstress.core;

import static dev.szx.dimensionworks.mekstress.core.CrossDimensionStressAllocator.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrossDimensionStressAllocatorTest {
    @Test void findsCrossDimensionAllocationInsteadOfGreedyStarvation() {
        var result = allocate(List.of(new Input("a", 10, 64), new Input("b", 10, 64)),
            List.of(new Output("c", "c", 10, Priority.HIGH), new Output("b", "b", 10, Priority.HIGH)));
        assertEquals(Status.OK, result.status());
        assertEquals(Set.of("c", "b"), result.running());
    }

    @Test void rejectsSameDimensionAndStopsWholePriorityAndAllLower() {
        var result = allocate(List.of(new Input("a", 10, 64)), List.of(
            new Output("high", "b", 4, Priority.HIGH), new Output("same", "a", 1, Priority.NORMAL),
            new Output("other", "b", 1, Priority.NORMAL), new Output("low", "b", 1, Priority.LOW)));
        assertEquals(Status.INSUFFICIENT, result.status());
        assertEquals(Set.of("high"), result.running());
    }

    @Test void rematchesHigherPriorityToAvoidUnnecessaryLowerPriorityStop() {
        var result = allocate(List.of(new Input("a", 10, 64), new Input("b", 10, 64)),
            List.of(new Output("high", "c", 10, Priority.HIGH),
                new Output("normal", "b", 10, Priority.NORMAL)));
        assertEquals(Status.OK, result.status());
        assertEquals(Set.of("high", "normal"), result.running());
    }

    @Test void mismatchAndOvercommitDoNotProduceOutputs() {
        var result = allocate(List.of(new Input("a", 10, 64), new Input("b", 10, 32)),
            List.of(new Output("x", "c", 1, Priority.HIGH)));
        assertEquals(Status.RPM_MISMATCH, result.status());
        assertTrue(result.running().isEmpty());
        var overflow = allocate(List.of(new Input("a", Long.MAX_VALUE, 1), new Input("a", 1, 1)), List.of());
        assertEquals(Status.NUMERIC_LIMIT, overflow.status());
        assertTrue(overflow.running().isEmpty());
    }

    @Test void distinctOutputRequestsCanExceedLongWithoutWrapping() {
        var result = allocate(List.of(new Input("a", Long.MAX_VALUE, 1)), List.of(
            new Output("x", "b", Long.MAX_VALUE, Priority.HIGH),
            new Output("y", "c", 1, Priority.HIGH)));
        assertEquals(Status.INSUFFICIENT, result.status());
        assertTrue(result.running().isEmpty());
    }
}
