package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MachineTypeTest {

    @Test
    void abyssalUsesRequiredIds() {
        MachineType abyssal = MachineType.ABYSSAL;

        assertEquals("abyssal_pressure_tower", abyssal.id());
        assertEquals("abyssal_brine", abyssal.fluidId());
        assertEquals("pressure_matrix", abyssal.matrixId());
        assertEquals("pressure_residue", abyssal.residueId());
        assertSame(FactoryDimension.ABYSSAL, abyssal.dimension());
    }

    @Test
    void everyMachineHasTwoModulesInItsDimension() {
        for (MachineType type : MachineType.values()) {
            assertEquals(2, type.modules().size());
            for (FactoryModule module : type.modules()) {
                assertSame(type.dimension(), module.dimension());
            }
        }
    }
}
