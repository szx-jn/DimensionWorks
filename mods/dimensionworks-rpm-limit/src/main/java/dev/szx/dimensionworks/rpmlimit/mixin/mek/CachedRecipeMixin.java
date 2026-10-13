package dev.szx.dimensionworks.rpmlimit.mixin.mek;

import dev.szx.dimensionworks.rpmlimit.quota.MekProcessingContext;
import dev.szx.dimensionworks.rpmlimit.quota.MekProcessingGate;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gates the actual operation count after Mekanism has calculated input and output capacity. */
@Mixin(value = CachedRecipe.class, remap = false)
public abstract class CachedRecipeMixin {

    @Redirect(
        method = "process",
        at = @At(
            value = "FIELD",
            target = "Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker;currentMax:I",
            opcode = Opcodes.GETFIELD
        ),
        remap = false
    )
    private int dimensionworks$gateOperations(OperationTracker tracker) {
        int requested = ((CachedRecipeOperationTrackerAccessor) (Object) tracker)
            .dimensionworks$getCurrentMax();
        CachedRecipe<?> cachedRecipe = (CachedRecipe<?>) (Object) this;
        MekanismRecipe recipe = cachedRecipe.getRecipe();
        return MekProcessingGate.allowOperations(
            requested, recipe, MekProcessingContext.current());
    }
}
