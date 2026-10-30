package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.MachinePowerRouteScope;
import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import dev.szx.dimensionworks.mekstress.core.WorkScheduler;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMixin {
    @Unique
    private static final Map<TileEntityMekanism, WorkScheduler.Advance> dimensionworks$workAdvances =
        new WeakHashMap<>();

    @Redirect(
        method = "tickServer",
        at = @At(
            value = "INVOKE",
            target = "Lmekanism/common/tile/base/TileEntityMekanism;onUpdateServer()V"
        ),
        remap = false
    )
    private static void dimensionworks$scheduleAndWrapUpdate(TileEntityMekanism tile) {
        TileEntityMekanismInvoker invoker = (TileEntityMekanismInvoker) tile;
        if (!StressRules.isEligible(tile)) {
            invoker.dimensionworks$onUpdateServer();
            return;
        }

        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            invoker.dimensionworks$onUpdateServer();
            return;
        }

        long tick = level.getGameTime();
        MachinePowerManager.DirectSource route = MachinePowerManager.powerRoute(tile);
        try (MachinePowerRouteScope.Scope ignored = MachinePowerRouteScope.push(tile, route)) {
            MachinePowerManager.updateDirectStress(tile, route);

            double workRate = MachinePowerManager.workRate(tile, route);
            WorkScheduler.Advance advance = WorkScheduler.advance(
                workRate, tick, dimensionworks$workAdvances.get(tile));
            dimensionworks$workAdvances.put(tile, advance);
            if (advance.calls() <= 0) {
                return;
            }

            try (MachineWorkContext.Scope ignoredWork = MachineWorkContext.push(
                MachinePowerManager.productionMultiplier(tile, route))) {
                for (int i = 0; i < advance.calls(); i++) {
                    invoker.dimensionworks$onUpdateServer();
                }
            }
        }
    }

    @Inject(method = "blockRemoved", at = @At("HEAD"), remap = false)
    private void dimensionworks$clearRemovedBlock(CallbackInfo ci) {
        dimensionworks$clearMechanicalWorkState((TileEntityMekanism) (Object) this);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), remap = true)
    private void dimensionworks$clearUnloadedBlock(CallbackInfo ci) {
        dimensionworks$clearMechanicalWorkState((TileEntityMekanism) (Object) this);
    }

    @Unique
    private static void dimensionworks$clearMechanicalWorkState(TileEntityMekanism tile) {
        dimensionworks$workAdvances.remove(tile);
        MachinePowerManager.clearForRemoval(tile);
    }
}
