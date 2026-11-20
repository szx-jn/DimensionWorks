package dev.szx.dimensionworks.jeiticks.client;

import java.util.Map;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.infrastructure.config.AllConfigs;

public final class RecipeTimeResolver {
    private RecipeTimeResolver() {}

    public static long resolve(IRecipeCategory<?> category, Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder) recipe = holder.value();
        if (category instanceof ProcessingViaFanCategory<?>) {
            // A vanilla smelting recipe's duration is not its fan-processing time.
            return Math.max(0, AllConfigs.server().kinetics.fanProcessingTime.get());
        }
        if (category.getRecipeType().equals(RecipeTypes.BREWING)) {
            return 400; // BrewingStandBlockEntity's single brewing operation, not brewing steps.
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            int ticks = cooking.getCookingTime();
            if (ticks > 0) {
                return ticks;
            }
            return switch (category.getRecipeType().getUid().toString()) {
                case "minecraft:smelting" -> 200;
                case "minecraft:blasting", "minecraft:smoking" -> 100;
                case "minecraft:campfire" -> 600;
                default -> 0;
            };
        }
        long ticks = DurationAccess.read(recipe);
        if (ticks > 0) {
            return ticks;
        }
        var uid = category.getRecipeType().getUid();
        if (uid.getNamespace().equals("mekanism")) {
            return MekanismTimes.BASE_TICKS.getOrDefault(uid, 0);
        }
        return 0;
    }
    // Lazy: optional Mekanism classes are only linked for an actual Mekanism category.
    private static final class MekanismTimes {
        private static final Map<net.minecraft.resources.ResourceLocation, Integer> BASE_TICKS = Map.ofEntries(
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.ENRICHING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.CRUSHING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.SMELTING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.COMPRESSING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.COMBINING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.METALLURGIC_INFUSING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.PURIFYING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.INJECTING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.SAWING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.CRYSTALLIZING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.DISSOLUTION.id(), 100),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.OXIDIZING.id(), 100),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.PIGMENT_EXTRACTING.id(), 100),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.PAINTING.id(), 200),
            Map.entry(mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION.id(), 100)
        );
    }
}
