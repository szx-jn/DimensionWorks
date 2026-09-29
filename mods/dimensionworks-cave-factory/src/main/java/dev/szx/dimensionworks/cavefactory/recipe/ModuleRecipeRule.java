package dev.szx.dimensionworks.cavefactory.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ModuleRecipeRule implements Recipe<Container> {
    private final ResourceLocation id;
    private final FactoryModule module;
    private final Set<ResourceLocation> recipeIds;

    public ModuleRecipeRule(ResourceLocation id, FactoryModule module, Set<ResourceLocation> recipeIds) {
        this.id = id;
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
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
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
    public ResourceLocation getId() {
        return id;
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
        @Override
        public ModuleRecipeRule fromJson(ResourceLocation id, JsonObject json) {
            String moduleName = GsonHelper.getAsString(json, "module");
            FactoryModule module = null;
            for (FactoryModule candidate : FactoryModule.values()) {
                if (candidate.id().equals(moduleName) || candidate.name().equalsIgnoreCase(moduleName)) {
                    module = candidate;
                    break;
                }
            }
            if (module == null) {
                throw new IllegalArgumentException("Unknown module " + moduleName);
            }
            JsonArray recipes = GsonHelper.getAsJsonArray(json, "recipes");
            Set<ResourceLocation> ids = new LinkedHashSet<>();
            for (var element : recipes) {
                ids.add(new ResourceLocation(element.getAsString()));
            }
            return new ModuleRecipeRule(id, module, ids);
        }

        @Override
        public ModuleRecipeRule fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            FactoryModule module = FactoryModule.values()[buffer.readVarInt()];
            int count = buffer.readVarInt();
            Set<ResourceLocation> ids = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                ids.add(buffer.readResourceLocation());
            }
            return new ModuleRecipeRule(id, module, ids);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ModuleRecipeRule recipe) {
            buffer.writeVarInt(recipe.module.ordinal());
            buffer.writeVarInt(recipe.recipeIds.size());
            for (ResourceLocation recipeId : recipe.recipeIds) {
                buffer.writeResourceLocation(recipeId);
            }
        }
    }
}
