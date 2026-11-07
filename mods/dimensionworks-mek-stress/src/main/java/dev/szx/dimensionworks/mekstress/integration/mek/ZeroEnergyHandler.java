package dev.szx.dimensionworks.mekstress.integration.mek;

import java.util.Collections;
import java.util.List;
import mekanism.api.NBTConstants;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.energy.item.ItemStackEnergyHandler;
import mekanism.common.util.ItemDataUtils;
import net.minecraft.world.item.ItemStack;

/** Replaces the normal Mekanism item energy handler for the DimensionWorks tablet. */
public final class ZeroEnergyHandler extends ItemStackEnergyHandler {

    private final List<IEnergyContainer> containers = Collections.singletonList(new ZeroEnergyContainer());

    public static void scrub(ItemStack stack) {
        ItemDataUtils.writeContainers(stack, NBTConstants.ENERGY_CONTAINERS,
            Collections.singletonList(new ZeroEnergyContainer()));
    }

    @Override
    protected List<IEnergyContainer> getInitialContainers() {
        return containers;
    }

    @Override
    protected void load() {
        super.load();
        scrub(getStack());
    }
}
