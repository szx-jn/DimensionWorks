package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only diagnostic access; never recalculate while observing a fault. */
@Mixin(value = KineticNetwork.class, remap = false)
public interface KineticNetworkAccessor {
    @Accessor("currentCapacity") float dimensionworks$currentCapacity();
    @Accessor("currentStress") float dimensionworks$currentStress();
    @Accessor("unloadedCapacity") float dimensionworks$unloadedCapacity();
    @Accessor("unloadedStress") float dimensionworks$unloadedStress();
    @Accessor("unloadedMembers") int dimensionworks$unloadedMembers();
    @Accessor("unloadedCapacity") void dimensionworks$setUnloadedCapacity(float value);
    @Accessor("unloadedStress") void dimensionworks$setUnloadedStress(float value);
    @Accessor("unloadedMembers") void dimensionworks$setUnloadedMembers(int value);
}
