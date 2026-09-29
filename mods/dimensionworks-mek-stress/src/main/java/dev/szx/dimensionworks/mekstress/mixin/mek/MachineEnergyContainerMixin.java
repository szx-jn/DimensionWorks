package dev.szx.dimensionworks.mekstress.mixin.mek;

import appeng.api.config.Actionable;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.ProcessingEnergyRegistry;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class MachineEnergyContainerMixin {
    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectExternalFe(FloatingLong amount, Action action, AutomationType automationType,
                                                  CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile != null && StressRules.isEligible(tile) && automationType != AutomationType.INTERNAL) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$useStressPower(FloatingLong amount, Action action, AutomationType automationType,
                                               CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$machineTile();
        if (tile == null || !StressRules.isEligible(tile) || amount.isZero()) {
            return;
        }
        if (automationType != AutomationType.INTERNAL) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }
        long tick = tile.getLevel() == null ? 0L : tile.getLevel().getGameTime();
        Actionable mode = action == Action.SIMULATE ? Actionable.SIMULATE : Actionable.MODULATE;
        if (!MachinePowerManager.consumeForProcessing(tile, tick, mode)) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }
        if (action != Action.SIMULATE && tile.getLevel() != null) {
            long requestedFe = Math.max(1L, amount.ceil().longValue());
            ProcessingEnergyRegistry.record(
                GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos()), tick, requestedFe);
        }
        cir.setReturnValue(amount);
    }

    private TileEntityMekanism dimensionworks$machineTile() {
        return this instanceof MachineEnergyContainerAccessor accessor
            ? accessor.dimensionworks$machineTile()
            : null;
    }
}
