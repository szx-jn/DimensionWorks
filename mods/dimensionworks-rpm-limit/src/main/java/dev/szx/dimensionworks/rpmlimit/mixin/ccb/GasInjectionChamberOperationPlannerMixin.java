package dev.szx.dimensionworks.rpmlimit.mixin.ccb;

import dev.szx.dimensionworks.rpmlimit.ccb.CcbGasInjectionReflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    targets = "net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner",
    remap = false
)
abstract class GasInjectionChamberOperationPlannerMixin {

    @Redirect(
        method = "createRecipePlan(Lnet/minecraft/world/item/ItemStack;Lnet/ty/createcraftedbeginning/api/gas/GasStack;Lnet/ty/createcraftedbeginning/recipe/GasInjectionRecipe;)Lnet/ty/createcraftedbeginning/content/airtights/gasinjectionchamber/GasInjectionChamberOperationPlanner$BeltPlan;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/ty/createcraftedbeginning/recipe/gas/consumption/GasConsumptionPlanner;findMaximumMultiplier(Lnet/ty/createcraftedbeginning/recipe/gas/GasRecipeRequirement;Lnet/ty/createcraftedbeginning/api/gas/pressure/GasPressureCompartment;I)I",
            remap = false
        ),
        remap = false
    )
    private int dimensionworks$limitBeltBatch(
        @Coerce Object gasRequirement,
        @Coerce Object gasTank,
        int maxInputCount
    ) {
        return CcbGasInjectionReflection.limitBeltBatch(
            this,
            gasRequirement,
            gasTank,
            maxInputCount
        );
    }
}
