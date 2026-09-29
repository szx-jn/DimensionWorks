package dev.szx.dimensionworks.mekstress.mixin.mek;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.content.matrix.MatrixEnergyContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MatrixEnergyContainer.class, remap = false)
public abstract class MatrixEnergyContainerMixin {

    @Inject(method = "getEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$getEnergy(CallbackInfoReturnable<FloatingLong> cir) {
        cir.setReturnValue(FloatingLong.ZERO);
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$insert(FloatingLong amount, Action action, AutomationType automationType,
                                       CallbackInfoReturnable<FloatingLong> cir) {
        cir.setReturnValue(amount);
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$extract(FloatingLong amount, Action action, AutomationType automationType,
                                        CallbackInfoReturnable<FloatingLong> cir) {
        cir.setReturnValue(FloatingLong.ZERO);
    }
}
