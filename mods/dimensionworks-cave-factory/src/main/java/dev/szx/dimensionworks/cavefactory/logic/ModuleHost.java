package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.items.ItemStackHandler;

public interface ModuleHost {
    Capability<ModuleHost> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    ItemStackHandler moduleInventory();

    MachineCapability machineCapability();

    default FactoryModule moduleAt(int slot) {
        return FactoryModule.byItem(moduleInventory().getStackInSlot(slot));
    }

    default boolean hasRecipeMapping(FactoryModule module) {
        return module != FactoryModule.PHASE_CONVERTER;
    }

    default boolean acceptsModule(FactoryModule module, boolean mechanismSlot) {
        if (module == null || module.kind() != (mechanismSlot ? ModuleKind.MECHANISM : ModuleKind.NUMERIC)) {
            return false;
        }
        if (!module.implementedThisMilestone()) {
            return false;
        }
        if (!ModuleCompatibility.supports(module, machineCapability())) {
            return false;
        }
        if (mechanismSlot && !hasRecipeMapping(module)) {
            return false;
        }
        return mechanismSlot
            ? ModuleRules.isAllowed(moduleAt(0), module)
            : ModuleRules.isAllowed(module, moduleAt(1));
    }

    default boolean ejectModules() {
        return false;
    }
}
