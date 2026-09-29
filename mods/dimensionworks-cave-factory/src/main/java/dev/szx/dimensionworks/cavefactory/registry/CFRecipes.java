package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.recipe.CaveProcessingRecipe;
import dev.szx.dimensionworks.cavefactory.recipe.FluidStabilizationRecipe;
import dev.szx.dimensionworks.cavefactory.recipe.ModuleRecipeRule;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;

public final class CFRecipes {
    public static RegistryObject<RecipeType<CaveProcessingRecipe>> CAVE_PROCESSING_TYPE;
    public static RegistryObject<RecipeType<FluidStabilizationRecipe>> FLUID_STABILIZATION_TYPE;
    public static RegistryObject<RecipeType<ModuleRecipeRule>> MODULE_RECIPE_RULE_TYPE;

    public static RegistryObject<RecipeSerializer<CaveProcessingRecipe>> CAVE_PROCESSING_SERIALIZER;
    public static RegistryObject<RecipeSerializer<FluidStabilizationRecipe>> FLUID_STABILIZATION_SERIALIZER;
    public static RegistryObject<RecipeSerializer<ModuleRecipeRule>> MODULE_RECIPE_RULE_SERIALIZER;

    private CFRecipes() {}

    public static void register() {
        CAVE_PROCESSING_TYPE = DimensionWorksCaveFactory.CFRegistry.RECIPE_TYPES.register(
            "cave_processing",
            () -> RecipeType.simple(DimensionWorksCaveFactory.id("cave_processing"))
        );
        FLUID_STABILIZATION_TYPE = DimensionWorksCaveFactory.CFRegistry.RECIPE_TYPES.register(
            "fluid_stabilization",
            () -> RecipeType.simple(DimensionWorksCaveFactory.id("fluid_stabilization"))
        );
        MODULE_RECIPE_RULE_TYPE = DimensionWorksCaveFactory.CFRegistry.RECIPE_TYPES.register(
            "module_recipe_rule",
            () -> RecipeType.simple(DimensionWorksCaveFactory.id("module_recipe_rule"))
        );

        CAVE_PROCESSING_SERIALIZER = DimensionWorksCaveFactory.CFRegistry.RECIPE_SERIALIZERS.register(
            "cave_processing",
            CaveProcessingRecipe.Serializer::new
        );
        FLUID_STABILIZATION_SERIALIZER = DimensionWorksCaveFactory.CFRegistry.RECIPE_SERIALIZERS.register(
            "fluid_stabilization",
            FluidStabilizationRecipe.Serializer::new
        );
        MODULE_RECIPE_RULE_SERIALIZER = DimensionWorksCaveFactory.CFRegistry.RECIPE_SERIALIZERS.register(
            "module_recipe_rule",
            ModuleRecipeRule.Serializer::new
        );
    }
}
