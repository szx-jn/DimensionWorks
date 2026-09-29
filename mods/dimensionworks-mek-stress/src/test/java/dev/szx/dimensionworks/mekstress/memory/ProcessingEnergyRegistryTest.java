package dev.szx.dimensionworks.mekstress.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessingEnergyRegistryTest {
    private static final Object MACHINE = new Object();

    @BeforeEach
    void clearRegistry() {
        ProcessingEnergyRegistry.clear(MACHINE);
    }

    @Test
    void recordsAndSumsFeWithinOneTick() {
        ProcessingEnergyRegistry.record(MACHINE, 10L, 5L);
        ProcessingEnergyRegistry.record(MACHINE, 10L, 3L);

        assertEquals(4L, ProcessingEnergyRegistry.convertedSu(MACHINE, 10L, 2.5D));
        assertEquals(0L, ProcessingEnergyRegistry.convertedSu(MACHINE, 11L, 2.5D));
    }

    @Test
    void aNewTickReplacesThePreviousRequest() {
        ProcessingEnergyRegistry.record(MACHINE, 10L, 250L);
        ProcessingEnergyRegistry.record(MACHINE, 11L, 5L);

        assertEquals(0L, ProcessingEnergyRegistry.convertedSu(MACHINE, 10L, 2.5D));
        assertEquals(2L, ProcessingEnergyRegistry.convertedSu(MACHINE, 11L, 2.5D));
    }
}
