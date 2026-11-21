package dev.szx.dimensionworks.maidaecrank.mixin;

import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import studio.fantasyit.maid_storage_manager.craft.generator.type.create.GeneratorCreate;

import java.util.List;

@Mixin(value = GeneratorCreate.class, remap = false)
public abstract class MaidStorageCreateRecipeCacheMixin {
    @Redirect(
            method = "onCache",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager;getAllRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;"
            ),
            require = 1
    )
    private List<? extends RecipeHolder<?>> dimensionworks$filterIncompatibleCreateRecipes(
            RecipeManager manager,
            net.minecraft.world.item.crafting.RecipeType<?> type
    ) {
        List<? extends RecipeHolder<?>> recipes = recipesFor(manager, type);
        Class<?> generator = ((Object) this).getClass();
        Class<?> expected = generator.getName().endsWith("GeneratorCreateCompact")
                ? CompactingRecipe.class
                : generator.getName().endsWith("GeneratorCreateMix")
                ? MixingRecipe.class
                : null;
        if (expected == null) {
            return recipes;
        }
        return recipes.stream()
                .filter(holder -> expected.isInstance(holder.value()))
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<? extends RecipeHolder<?>> recipesFor(
            RecipeManager manager,
            net.minecraft.world.item.crafting.RecipeType<?> type
    ) {
        return manager.getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) type);
    }
}
