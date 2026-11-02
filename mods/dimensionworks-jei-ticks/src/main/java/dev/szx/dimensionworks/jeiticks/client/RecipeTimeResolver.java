package dev.szx.dimensionworks.jeiticks.client;

import java.util.Map;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;

public final class RecipeTimeResolver {
    // Mekanism 10.4.16.80 machine constructors, not BaseRecipeCategory's 20-tick UI timer.
    private static final Map<String, Integer> MEKANISM_BASE_TICKS = Map.ofEntries(
        Map.entry("enrichment_chamber", 200), Map.entry("crusher", 200),
        Map.entry("energized_smelter", 200), Map.entry("osmium_compressor", 200),
        Map.entry("combiner", 200), Map.entry("metallurgic_infuser", 200),
        Map.entry("purification_chamber", 200), Map.entry("chemical_injection_chamber", 200),
        Map.entry("precision_sawmill", 200), Map.entry("chemical_crystallizer", 200),
        Map.entry("chemical_dissolution_chamber", 100), Map.entry("chemical_oxidizer", 100),
        Map.entry("pigment_extractor", 100), Map.entry("painting_machine", 200),
        Map.entry("nutritional_liquifier", 100)
    );

    private RecipeTimeResolver() {}

    public static long resolve(IRecipeCategory<?> category, Object recipe) {
        if (DurationAccess.inherits(category,
                "com.simibubi.create.compat.jei.category.ProcessingViaFanCategory")) {
            // A vanilla smelting recipe's time is NOT its fan-processing time.
            try {
                Object server = Class.forName("com.simibubi.create.infrastructure.config.AllConfigs")
                    .getMethod("server").invoke(null);
                Object kinetics = server.getClass().getField("kinetics").get(server);
                Object setting = kinetics.getClass().getField("fanProcessingTime").get(kinetics);
                return Math.max(0, ((Number) setting.getClass().getMethod("get").invoke(setting)).longValue());
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return 0;
            }
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
            return MEKANISM_BASE_TICKS.getOrDefault(uid.getPath(), 0);
        }
        return 0;
    }
}
