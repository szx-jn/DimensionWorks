package dev.szx.dimensionworks.mekstress.integration.mek;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import net.minecraft.nbt.CompoundTag;

/** An energy container that deliberately cannot store or transfer energy. */
public final class ZeroEnergyContainer implements IEnergyContainer {

    @Override
    public FloatingLong getEnergy() {
        return FloatingLong.ZERO;
    }

    @Override
    public void setEnergy(FloatingLong energy) {
        // The item is a manufacturing component, not a battery.
    }

    @Override
    public FloatingLong insert(FloatingLong amount, Action action, AutomationType automationType) {
        return amount.copy();
    }

    @Override
    public FloatingLong extract(FloatingLong amount, Action action, AutomationType automationType) {
        return FloatingLong.ZERO;
    }

    @Override
    public FloatingLong getMaxEnergy() {
        return FloatingLong.ZERO;
    }

    @Override
    public void onContentsChanged() {
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // This disabled energy container never restores stored charge.
    }
}
