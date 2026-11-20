package dev.szx.dimensionworks.mekstress.integration.mek;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import net.minecraft.nbt.CompoundTag;

/** An energy container that deliberately cannot store or transfer energy. */
public final class ZeroEnergyContainer implements IEnergyContainer {

    @Override
    public long getEnergy() {
        return 0L;
    }

    @Override
    public void setEnergy(long energy) {
        // The item is a manufacturing component, not a battery.
    }

    @Override
    public long insert(long amount, Action action, AutomationType automationType) {
        return amount;
    }

    @Override
    public long extract(long amount, Action action, AutomationType automationType) {
        return 0L;
    }

    @Override
    public long getMaxEnergy() {
        return 0L;
    }

    @Override
    public void onContentsChanged() {
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag nbt) {
        // This disabled energy container never restores stored charge.
    }
}
