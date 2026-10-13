package dev.szx.dimensionworks.rpmlimit.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import dev.szx.dimensionworks.rpmlimit.quota.AeCraftingGate;
import dev.szx.dimensionworks.rpmlimit.quota.AeCraftingRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gates final AE2 output before CraftingCpuLogic mutates waitingFor or remainingAmount. */
@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin {

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateFinalOutput(AEKey what, long amount, Actionable type,
                                                CallbackInfoReturnable<Long> cir) {
        if (type != Actionable.MODULATE || amount <= 0L) {
            return;
        }

        CraftingCpuLogic self = (CraftingCpuLogic) (Object) this;
        GenericStack finalOutput = self.getFinalJobOutput();
        if (finalOutput == null || !what.equals(finalOutput.what())) {
            return;
        }

        long deliverable = Math.min(amount, self.getWaitingFor(what));
        if (deliverable <= 0L) {
            cir.setReturnValue(0L);
            cir.cancel();
            return;
        }

        var cluster = ((CraftingCpuLogicClusterAccessor) (Object) self)
            .dimensionworks$getCluster();
        var owner = AeCraftingRegistry.owner(cluster == null ? null : cluster.getNode());
        long tick = cluster == null || cluster.getLevel() == null
            ? 0L : cluster.getLevel().getGameTime();
        if (!AeCraftingGate.allowFinalOutput(owner, what, deliverable, tick)) {
            cir.setReturnValue(0L);
            cir.cancel();
        }
    }
}
