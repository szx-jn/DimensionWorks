package dev.szx.dimensionworks.cavefactory.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;

import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import net.minecraft.core.NonNullList;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class ModuleRecipeRule implements Recipe<net.minecraft.world.item.crafting.RecipeInput> {
    private final FactoryModule module;
    private final Set<ResourceLocation> recipeIds;

    public ModuleRecipeRule(FactoryModule module, Set<ResourceLocation> recipeIds) {
        this.module = module;
        this.recipeIds = Set.copyOf(recipeIds);
    }

    public FactoryModule module() {
        return module;
    }

    public Set<ResourceLocation> recipeIds() {
        return recipeIds;
    }

    public boolean appliesTo(ResourceLocation recipeId) {
        return recipeIds.contains(recipeId);
    }

    @Override
    public boolean matches(net.minecraft.world.item.crafting.RecipeInput container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.item.crafting.RecipeInput container, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.MODULE_RECIPE_RULE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.MODULE_RECIPE_RULE_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<ModuleRecipeRule> {
        private static final MapCodec<ModuleRecipeRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FactoryRecipeCodecs.MODULE.fieldOf("module").forGetter(ModuleRecipeRule::module),
            ResourceLocation.CODEC.listOf().fieldOf("recipes").forGetter(r -> r.recipeIds.stream().sorted().toList())
        ).apply(instance, (module, ids) -> new ModuleRecipeRule(module, Set.copyOf(ids))));
        private static final StreamCodec<RegistryFriendlyByteBuf, ModuleRecipeRule> STREAM =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<ModuleRecipeRule> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ModuleRecipeRule> streamCodec() { return STREAM; }
    }
}
