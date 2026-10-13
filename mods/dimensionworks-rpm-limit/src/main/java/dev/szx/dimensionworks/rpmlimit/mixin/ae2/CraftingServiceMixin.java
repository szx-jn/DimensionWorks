package dev.szx.dimensionworks.rpmlimit.mixin.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.service.CraftingService;
import dev.szx.dimensionworks.rpmlimit.quota.AeCraftingGate;
import dev.szx.dimensionworks.rpmlimit.quota.AeCraftingRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gates non-simulation AE2 submissions after the native simulation branch. */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dimensionworks$registerCraftingService(IGrid grid,
                                                        appeng.api.networking.storage.IStorageService storage,
                                                        appeng.api.networking.energy.IEnergyService energy,
                                                        CallbackInfo ci) {
        CraftingService service = (CraftingService) (Object) this;
        AeCraftingRegistry.register(service, AeCraftingRegistry.owner(grid == null
            ? null : grid.getPivot()));
    }

    @Inject(method = "submitJob", at = @At("HEAD"), remap = false)
    private void dimensionworks$gateSubmission(ICraftingPlan job,
                                               ICraftingRequester requestingMachine,
                                               ICraftingCPU target,
                                               boolean prioritizePower,
                                               IActionSource src,
                                               CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (job.simulation()) {
            return;
        }

        CraftingService service = (CraftingService) (Object) this;
        IGrid grid = ((CraftingServiceAccessor) (Object) service).dimensionworks$getGrid();
        var owner = AeCraftingRegistry.owner(grid == null ? null : grid.getPivot());
        if (!AeCraftingGate.allowSubmission(service, job, owner, 0L)) {
            cir.setReturnValue(CraftingSubmitResult.NO_CPU_FOUND);
            cir.cancel();
        }
    }
}
