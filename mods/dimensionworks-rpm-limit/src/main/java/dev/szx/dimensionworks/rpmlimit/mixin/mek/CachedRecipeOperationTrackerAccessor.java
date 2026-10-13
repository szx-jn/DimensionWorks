package dev.szx.dimensionworks.rpmlimit.mixin.mek;

import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OperationTracker.class, remap = false)
public interface CachedRecipeOperationTrackerAccessor {

    @Accessor("currentMax")
    int dimensionworks$getCurrentMax();
}
