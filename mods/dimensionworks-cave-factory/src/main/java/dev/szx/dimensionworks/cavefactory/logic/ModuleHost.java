package dev.szx.dimensionworks.cavefactory.logic;

import net.neoforged.neoforge.items.ItemStackHandler;

public interface ModuleHost {
    net.neoforged.neoforge.capabilities.BlockCapability<ModuleHost, net.minecraft.core.Direction> CAPABILITY =
        net.neoforged.neoforge.capabilities.BlockCapability.createSided(
            dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory.id("module_host"), ModuleHost.class);

    static ModuleHost find(net.minecraft.world.level.block.entity.BlockEntity owner) {
        return owner.getLevel() == null || owner.isRemoved() ? null
            : owner.getLevel().getCapability(CAPABILITY, owner.getBlockPos(), null);
    }

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
