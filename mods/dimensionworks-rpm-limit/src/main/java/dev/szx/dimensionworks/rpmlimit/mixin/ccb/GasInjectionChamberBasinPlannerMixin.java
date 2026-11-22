package dev.szx.dimensionworks.rpmlimit.mixin.ccb;

import dev.szx.dimensionworks.rpmlimit.ccb.CcbGasInjectionReflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    targets = "net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner",
    remap = false
)
abstract class GasInjectionChamberBasinPlannerMixin {

    @Inject(
        method = "getMaxBatchSize(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/neoforged/neoforge/items/IItemHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/ty/createcraftedbeginning/recipe/GasInjectionRecipe;)I",
        at = @At("RETURN"),
        remap = false
    )
    private void dimensionworks$limitBatch(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(CcbGasInjectionReflection.limitBasinBatch(this, callback.getReturnValueI()));
    }
}
