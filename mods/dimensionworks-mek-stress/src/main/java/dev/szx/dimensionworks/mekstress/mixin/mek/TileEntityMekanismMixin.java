package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.WorkScheduler;
import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import dev.szx.dimensionworks.mekstress.core.MachinePowerRouteScope;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMixin {
    @Unique
    private boolean dimensionworks$repeatingUpdate;
    @Unique
    private WorkScheduler.Advance dimensionworks$workAdvance;

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
        MachinePowerManager.DirectSource route = MachinePowerManager.powerRoute(self);
        try (MachinePowerRouteScope.Scope ignored = MachinePowerRouteScope.push(self, route)) {
            MachinePowerManager.updateDirectStress(self, route);
            MachinePowerManager.updateMechanicalEnergy(self, route);

            double workRate = MachinePowerManager.workRate(self, route);
            dimensionworks$workAdvance = WorkScheduler.advance(workRate, tick, dimensionworks$workAdvance);
            int calls = dimensionworks$workAdvance.calls();
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
    }

    /**
     * Keep the production context active for the complete Mekanism tick.
     *
     * <p>The previous redirect only wrapped the direct onUpdateServer call site. Scoping the
     * static tick entry also covers recipe handlers that execute elsewhere in the same tick
     * and keeps the context independent of which machine subclass overrides onUpdateServer.
     */
    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private static void dimensionworks$pushProductionContext(Level level, BlockPos pos,
                                                              BlockState state, TileEntityMekanism tile,
                                                              CallbackInfo ci) {
        double multiplier = StressRules.isEligible(tile)
            ? MachinePowerManager.productionMultiplier(tile)
            : 1.0D;
        MachineWorkContext.push(multiplier);
    }

    @Inject(method = "tickServer", at = @At("RETURN"), remap = false)
    private static void dimensionworks$popProductionContext(Level level, BlockPos pos,
                                                             BlockState state, TileEntityMekanism tile,
                                                             CallbackInfo ci) {
        MachineWorkContext.pop();
    }

    @Inject(method = "blockRemoved", at = @At("HEAD"), remap = false)
    private void dimensionworks$clearRemovedBlock(CallbackInfo ci) {
        MachinePowerManager.clearForRemoval((TileEntityMekanism) (Object) this);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), remap = true)
    private void dimensionworks$clearUnloadedBlock(CallbackInfo ci) {
        MachinePowerManager.clearForRemoval((TileEntityMekanism) (Object) this);
    }
}
