package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.WorkScheduler;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMixin {
    @Unique
    private boolean dimensionworks$repeatingUpdate;

    @Inject(method = "onUpdateServer", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$scheduleMechanicalWork(CallbackInfo ci) {
        if (dimensionworks$repeatingUpdate) {
            return;
        }
        TileEntityMekanism self = (TileEntityMekanism) (Object) this;
        if (!StressRules.isEligible(self)) {
            return;
        }
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long tick = level.getGameTime();
        MachinePowerManager.DirectSource direct = MachinePowerManager.directSource(self);
        MachinePowerManager.updateDirectStress(self, direct, tick);

        double workRate = MachinePowerManager.workRate(self, tick);
        int calls = WorkScheduler.callsForRate(workRate, tick);
        if (calls <= 0) {
            ci.cancel();
            return;
        }
        if (calls == 1) {
            return;
        }

        ci.cancel();
        dimensionworks$repeatingUpdate = true;
        try {
            TileEntityMekanismInvoker invoker = (TileEntityMekanismInvoker) this;
            for (int i = 0; i < calls; i++) {
                invoker.dimensionworks$onUpdateServer();
            }
        } finally {
            dimensionworks$repeatingUpdate = false;
        }
    }
    @Inject(method = "blockRemoved", at = @At("HEAD"), remap = false)
    private void dimensionworks$clearRemovedBlock(CallbackInfo ci) {
        MachinePowerManager.clearForRemoval((TileEntityMekanism) (Object) this);
    }
}
