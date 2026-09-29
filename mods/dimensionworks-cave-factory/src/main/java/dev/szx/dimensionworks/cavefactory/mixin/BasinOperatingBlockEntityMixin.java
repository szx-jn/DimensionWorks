package dev.szx.dimensionworks.cavefactory.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHost;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {
    @Redirect(
        remap = false,
        method = "applyBasinRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/basin/BasinRecipe;apply"
                + "(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;"
                + "Lnet/minecraft/world/item/crafting/Recipe;)Z"
        )
    )
    private boolean dimensionworks$applyMappedRecipeBonus(BasinBlockEntity basin, Recipe<?> recipe) {
        boolean applied = BasinRecipe.apply(basin, recipe);
        if (applied) {
            CreateModuleHost.applyBasinBonus((BasinOperatingBlockEntity) (Object) this, basin, recipe);
        }
        return applied;
    }
}
