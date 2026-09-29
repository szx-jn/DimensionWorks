package dev.szx.dimensionworks.cavefactory.logic;

public final class ModuleRules {
    private ModuleRules() {}

    public static boolean isAllowed(FactoryModule numeric, FactoryModule mechanism) {
        if (numeric == null || mechanism == null) {
            return true;
        }
        if (numeric.kind() != ModuleKind.NUMERIC || mechanism.kind() != ModuleKind.MECHANISM) {
            return false;
        }
        return !(numeric == FactoryModule.BIOMASS_YIELD_AMPLIFIER
            && mechanism == FactoryModule.BIOMASS_CULTIVATOR);
    }
}
