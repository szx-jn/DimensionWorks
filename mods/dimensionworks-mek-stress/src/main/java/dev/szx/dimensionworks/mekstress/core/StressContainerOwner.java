package dev.szx.dimensionworks.mekstress.core;

import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.Nullable;

/** Connects a Mekanism energy container back to its owning tile. */
public interface StressContainerOwner extends StressEnergyBuffer {

    void dimensionworks$setOwner(@Nullable TileEntityMekanism tile);

    @Nullable
    TileEntityMekanism dimensionworks$getOwner();
}
