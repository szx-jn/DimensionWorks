package dev.szx.dimensionworks.cavefactory.logic;

public final class ModuleCompatibility {
    private ModuleCompatibility() {}

    public static boolean supports(FactoryModule module, MachineCapability capability) {
        if (module == null || capability == null) {
            return false;
        }
        if (module.kind() == ModuleKind.NUMERIC) {
            return module != FactoryModule.PRESSURE_BUFFER || capability.hasFluidTanks();
        }
        return module == FactoryModule.PHASE_CONVERTER && capability.hasMappedRecipes();
    }

    public static boolean supportsCapacity(FactoryModule module, MachineCapability capability) {
        return module == FactoryModule.PRESSURE_BUFFER && capability.hasFluidTanks();
    }

    public static boolean supportsMechanismProcessing(FactoryModule module, MachineCapability capability) {
        return module == FactoryModule.PHASE_CONVERTER && capability.hasMappedRecipes();
    }
}
