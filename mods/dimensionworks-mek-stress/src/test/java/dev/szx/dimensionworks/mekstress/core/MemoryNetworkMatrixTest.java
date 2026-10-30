package dev.szx.dimensionworks.mekstress.core;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class MemoryNetworkMatrixTest {
    @Test void capacityComesFromInputsRegardlessOfMachineCountAndElapsedTime() {
        for (int count = 1; count <= 150; count++) {
            var budget = new LiveStressBudget<Integer>();
            budget.input(-1, count * 262144L);
            for (int machine = 0; machine < count; machine++) budget.output(machine, 262144);
            var snapshot = budget.snapshot(0);
            assertFalse(snapshot.overloaded());
            assertEquals(0, snapshot.availableSu());
            assertEquals(snapshot, budget.snapshot(0));
            budget.input(-1, count * 262144L - 1);
            assertTrue(budget.snapshot(0).overloaded());
        }
    }
}
