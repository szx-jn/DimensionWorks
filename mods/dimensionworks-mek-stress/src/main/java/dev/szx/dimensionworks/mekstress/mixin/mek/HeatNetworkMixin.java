package dev.szx.dimensionworks.mekstress.mixin.mek;

import mekanism.common.content.network.HeatNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeatNetwork.class, remap = false)
public abstract class HeatNetworkMixin {

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$disableHeatNetwork(CallbackInfo ci) {
        ci.cancel();
    }
}
