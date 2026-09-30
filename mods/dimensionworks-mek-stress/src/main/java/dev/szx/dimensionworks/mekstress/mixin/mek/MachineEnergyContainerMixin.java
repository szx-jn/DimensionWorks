package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class MachineEnergyContainerMixin {
    @Inject(method = "getEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$getMechanicalEnergy(CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile == null || !StressRules.isEligible(tile)) {
            return;
        }
        cir.setReturnValue(MachinePowerManager.hasMechanicalPower(tile)
            ? ((BasicEnergyContainer) (Object) this).getMaxEnergy()
            : FloatingLong.ZERO);
    }

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$isMechanicallyEmpty(CallbackInfoReturnable<Boolean> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile != null && StressRules.isEligible(tile)) {
            cir.setReturnValue(!MachinePowerManager.hasMechanicalPower(tile));
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectFe(FloatingLong amount, Action action, AutomationType automationType,
                                         CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile != null && StressRules.isEligible(tile)) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$extractMechanicalEnergy(FloatingLong amount, Action action, AutomationType automationType,
                                                       CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile == null || !StressRules.isEligible(tile) || amount.isZero()) {
            return;
        }
        if (automationType != AutomationType.INTERNAL || !MachinePowerManager.consumeForProcessing(tile)) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }
        cir.setReturnValue(amount);
    }

    private TileEntityMekanism dimensionworks$machineTile() {
        return this instanceof MachineEnergyContainerAccessor accessor
            ? accessor.dimensionworks$machineTile()
            : null;
    }
}
