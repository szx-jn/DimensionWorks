package dev.szx.dimensionworks.mekstress.mixin.draconic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disable all external OP and FE injection. ReactorPowerBridge is the SU-only input. */
@Mixin(targets = "com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector$OPInjector", remap = false)
public abstract class ReactorInjectorEnergyMixin {
    @Inject(method = "receiveOP", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectOp(long amount, boolean simulate, CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(0L);
    }

    @Inject(method = "receiveEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectFe(int amount, boolean simulate, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    // External storage reporting is also zero. The core's real operating
    // saturation remains internal and is never exposed as FE/OP.
    @Inject(method = "getMaxOPStored", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$hideOpCapacity(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(0L);
    }

    @Inject(method = "getMaxEnergyStored", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$hideFeCapacity(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "canReceive", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectCapability(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
