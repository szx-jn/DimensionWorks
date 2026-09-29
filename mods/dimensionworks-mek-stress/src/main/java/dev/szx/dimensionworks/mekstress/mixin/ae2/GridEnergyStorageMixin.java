package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.me.energy.GridEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GridEnergyStorage.class, remap = false)
public abstract class GridEnergyStorageMixin {

    @Shadow
    private int nodeCount;

    /**
     * FTB Ultimine can destroy the same cable bus twice in one chain operation. AE2 then removes
     * the already detached grid node again and throws, aborting the rest of the chain. The second
     * removal has no remaining work, so it is safe to ignore.
     */
    @Inject(method = "removeNode", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$skipDuplicateNodeRemoval(CallbackInfo ci) {
        if (nodeCount <= 0) {
            ci.cancel();
        }
    }
}
