package dev.szx.dimensionworks.mekstress.mixin.draconic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disable capability-based extraction so FE/OP cannot duplicate Create SU output. */
@Mixin(targets = "com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorStabilizer$OPExtractor", remap = false)
public abstract class ReactorStabilizerEnergyMixin {
    @Inject(method = "extractOP", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectOp(long amount, boolean simulate, CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(0L);
    }

    @Inject(method = "extractEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectFe(int amount, boolean simulate, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "canExtract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectCapability(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
