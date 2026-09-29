package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MachineEnergyContainer.class, remap = false)
public abstract class MachineEnergyContainerMixin {

    @Shadow
    @Final
    protected TileEntityMekanism tile;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dimensionworks$setOwner(CallbackInfo ci) {
        ((StressContainerOwner) (Object) this).dimensionworks$setOwner(tile);
    }
}
