package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.energy.LaserEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LaserEnergyContainer.class, remap = false)
public abstract class LaserEnergyContainerMixin {

    @Inject(method = "create", at = @At("RETURN"), remap = false)
    private static void dimensionworks$setOwner(Predicate<AutomationType> canExtract,
                                                Predicate<AutomationType> canInsert,
                                                TileEntityMekanism tile,
                                                @Nullable IContentsListener listener,
                                                CallbackInfoReturnable<LaserEnergyContainer> cir) {
        ((StressContainerOwner) cir.getReturnValue()).dimensionworks$setOwner(tile);
    }
}
