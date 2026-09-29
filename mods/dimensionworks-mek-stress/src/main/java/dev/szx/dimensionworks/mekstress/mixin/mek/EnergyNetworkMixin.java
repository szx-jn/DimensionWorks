package dev.szx.dimensionworks.mekstress.mixin.mek;

import mekanism.common.content.network.EnergyNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnergyNetwork.class, remap = false)
public abstract class EnergyNetworkMixin {

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$disableEnergyNetwork(CallbackInfo ci) {
        ci.cancel();
    }
}
