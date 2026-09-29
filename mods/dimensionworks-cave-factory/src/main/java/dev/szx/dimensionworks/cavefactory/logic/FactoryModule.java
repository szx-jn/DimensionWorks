package dev.szx.dimensionworks.cavefactory.logic;

import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import net.minecraft.world.item.ItemStack;

public enum FactoryModule {
    FLUX_CONSERVATOR("flux_conservator", FactoryDimension.MAGNETIC, ModuleKind.NUMERIC),
    POLARITY_INVERTER("polarity_inverter", FactoryDimension.MAGNETIC, ModuleKind.MECHANISM),
    BIOMASS_YIELD_AMPLIFIER("biomass_yield_amplifier", FactoryDimension.PRIMORDIAL, ModuleKind.NUMERIC),
    BIOMASS_CULTIVATOR("biomass_cultivator", FactoryDimension.PRIMORDIAL, ModuleKind.MECHANISM),
    ISOTOPE_ECONOMIZER("isotope_economizer", FactoryDimension.TOXIC, ModuleKind.NUMERIC),
    FISSION_RECLAIMER("fission_reclaimer", FactoryDimension.TOXIC, ModuleKind.MECHANISM),
    PRESSURE_BUFFER("pressure_buffer", FactoryDimension.ABYSSAL, ModuleKind.NUMERIC),
    PHASE_CONVERTER("phase_converter", FactoryDimension.ABYSSAL, ModuleKind.MECHANISM),
    UMBRAL_CAPACITY_CORE("umbral_capacity_core", FactoryDimension.FORLORN, ModuleKind.NUMERIC),
    PHASE_OVERBUFFER("phase_overbuffer", FactoryDimension.FORLORN, ModuleKind.MECHANISM),
    SUGAR_ECONOMIZER("sugar_economizer", FactoryDimension.CANDY, ModuleKind.NUMERIC),
    CRYSTAL_COMPRESSOR("crystal_compressor", FactoryDimension.CANDY, ModuleKind.MECHANISM);

    private final String id;
    private final FactoryDimension dimension;
    private final ModuleKind kind;

    FactoryModule(String id, FactoryDimension dimension, ModuleKind kind) {
        this.id = id;
        this.dimension = dimension;
        this.kind = kind;
    }

    public String id() {
        return id;
    }

    public FactoryDimension dimension() {
        return dimension;
    }

    public ModuleKind kind() {
        return kind;
    }

    public MachineCapability machineCapability() {
        if (kind == ModuleKind.NUMERIC) {
            return this == PRESSURE_BUFFER ? MachineCapability.ITEM_AND_FLUID_TANKS : MachineCapability.ITEM_ONLY;
        }
        return this == PHASE_CONVERTER ? MachineCapability.MAPPED_RECIPES : MachineCapability.NONE;
    }

    public static FactoryModule byItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (FactoryModule module : values()) {
            if (CFItems.module(module) != null && stack.is(CFItems.module(module).get())) {
                return module;
            }
        }
        return null;
    }

    public boolean isCompatible(MachineCapability capability) {
        return ModuleCompatibility.supports(this, capability);
    }

    public boolean implementedThisMilestone() {
        return this == PRESSURE_BUFFER || this == PHASE_CONVERTER;
    }
}
