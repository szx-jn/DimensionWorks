package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import dev.szx.dimensionworks.mekstress.integration.mek.RecipeEnergyOperations;
import mekanism.api.math.FloatingLong;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CachedRecipe.class, remap = false)
public abstract class CachedRecipeEnergyMixin {
    @Redirect(method = "calculateOperationsThisTick", at = @At(value = "INVOKE",
        target = "Lmekanism/api/math/FloatingLong;divideToInt(Lmekanism/api/math/FloatingLong;)I"), remap = false)
    private int dimensionworks$correctVirtualEnergyDivision(FloatingLong available, FloatingLong perOperation) {
        return MachineWorkContext.active()
            ? RecipeEnergyOperations.divide(available, perOperation)
            : available.divideToInt(perOperation);
    }
}
