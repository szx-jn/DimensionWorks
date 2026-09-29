package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleCompatibilityTest {

    @Test
    void pressureBufferAcceptsFluidCapacityMachinesOnly() {
        assertTrue(ModuleCompatibility.supportsCapacity(
            FactoryModule.PRESSURE_BUFFER, MachineCapability.ITEM_AND_FLUID_TANKS
        ));
        assertFalse(ModuleCompatibility.supportsCapacity(
            FactoryModule.PRESSURE_BUFFER, MachineCapability.ITEM_ONLY
        ));
    }

    @Test
    void pressureBufferScalesOnlyApplicableCapacity() {
        assertEquals(5_000, MachineTuning.scaleFluidCapacity(4_000, true));
        assertEquals(4_000, MachineTuning.scaleFluidCapacity(4_000, false));
    }

    @Test
    void phaseConverterRequiresMappedRecipeCapability() {
        assertTrue(ModuleCompatibility.supportsMechanismProcessing(
            FactoryModule.PHASE_CONVERTER, MachineCapability.MAPPED_RECIPES
        ));
        assertFalse(ModuleCompatibility.supportsMechanismProcessing(
            FactoryModule.PHASE_CONVERTER, MachineCapability.NONE
        ));
    }

    @Test
    void compositeCapabilitiesExposeBothContracts() {
        assertTrue(MachineCapability.ITEM_FLUID_AND_MAPPED_RECIPES.hasItemHandlers());
        assertTrue(MachineCapability.ITEM_FLUID_AND_MAPPED_RECIPES.hasFluidTanks());
        assertTrue(MachineCapability.ITEM_FLUID_AND_MAPPED_RECIPES.hasMappedRecipes());
    }
}
