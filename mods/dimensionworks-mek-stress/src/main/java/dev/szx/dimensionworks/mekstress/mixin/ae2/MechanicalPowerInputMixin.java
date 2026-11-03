package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnits;
import appeng.blockentity.powersink.AEBasePoweredBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AE2 and its powered machine subclasses use their AE grid, fed by the mechanical acceptor. */
@Mixin(value = AEBasePoweredBlockEntity.class, remap = false)
public abstract class MechanicalPowerInputMixin {
    @Inject(method = "injectExternalPower", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectExternalFe(PowerUnits unit, double amount, Actionable action,
        CallbackInfoReturnable<Double> cir) {
        if (unit == PowerUnits.FE || unit == PowerUnits.RF) cir.setReturnValue(amount);
    }

    @Inject(method = "getExternalPowerDemand", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$noFeDemand(PowerUnits unit, double maximum, CallbackInfoReturnable<Double> cir) {
        if (unit == PowerUnits.FE || unit == PowerUnits.RF) cir.setReturnValue(0D);
    }
}
