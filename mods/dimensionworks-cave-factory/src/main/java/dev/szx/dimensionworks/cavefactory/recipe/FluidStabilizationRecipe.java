package dev.szx.dimensionworks.cavefactory.recipe;

import com.google.gson.JsonObject;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class FluidStabilizationRecipe implements Recipe<CaveProcessingInput> {
    private final ResourceLocation id;
    private final net.minecraft.world.level.material.Fluid inputFluid;
    private final int inputAmount;
    private final ItemStack result;
    private final FactoryModule requiredModule;

    public FluidStabilizationRecipe(
        ResourceLocation id,
        net.minecraft.world.level.material.Fluid inputFluid,
        int inputAmount,
        ItemStack result,
        FactoryModule requiredModule
    ) {
        this.id = id;
        this.inputFluid = inputFluid;
        this.inputAmount = Math.max(1, inputAmount);
        this.result = result.copy();
        this.requiredModule = requiredModule;
    }

    public net.minecraft.world.level.material.Fluid inputFluid() {
        return inputFluid;
    }

    public int inputAmount() {
        return inputAmount;
    }

    public ItemStack result() {
        return result.copy();
    }

    public FactoryModule requiredModule() {
        return requiredModule;
    }

    public boolean matches(FluidStack fluid, FactoryModule module) {
        return (requiredModule == null || requiredModule == module)
            && !fluid.isEmpty()
            && fluid.getFluid() == inputFluid
            && fluid.getAmount() >= inputAmount;
    }

    @Override
    public boolean matches(CaveProcessingInput input, Level level) {
        return matches(input.fluidInput(), null);
    }

    @Override
    public ItemStack assemble(CaveProcessingInput input, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.FLUID_STABILIZATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.FLUID_STABILIZATION_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<FluidStabilizationRecipe> {
        @Override
        public FluidStabilizationRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonObject input = GsonHelper.getAsJsonObject(json, "input");
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(input, "fluid"));
            net.minecraft.world.level.material.Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || !CFFluids.isFactoryFluid(new FluidStack(fluid, 1))) {
                throw new IllegalArgumentException("Unknown factory fluid " + fluidId);
            }
            JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultJson, "item"));
            net.minecraft.world.item.Item outputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (outputItem == null || outputItem == net.minecraft.world.item.Items.AIR) {
                throw new IllegalArgumentException("Unknown output item " + itemId);
            }
            ItemStack result = new ItemStack(outputItem);
            result.setCount(Math.max(1, GsonHelper.getAsInt(resultJson, "count", 1)));
            String moduleName = GsonHelper.getAsString(json, "required_module", "");
            FactoryModule module = null;
            if (!moduleName.isBlank()) {
                for (FactoryModule candidate : FactoryModule.values()) {
                    if (candidate.id().equals(moduleName)) {
                        module = candidate;
                        break;
                    }
                }
                if (module == null) {
                    throw new IllegalArgumentException("Unknown required module " + moduleName);
                }
            }
            return new FluidStabilizationRecipe(
                id,
                fluid,
                GsonHelper.getAsInt(input, "amount", dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.stabilizationInput()),
                result,
                module
            );
        }

        @Override
        public FluidStabilizationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ResourceLocation fluidId = buffer.readResourceLocation();
            net.minecraft.world.level.material.Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || !CFFluids.isFactoryFluid(new FluidStack(fluid, 1))) {
                throw new IllegalArgumentException("Unknown factory fluid " + fluidId);
            }
            int amount = buffer.readVarInt();
            ItemStack result = buffer.readItem();
            FactoryModule module = null;
            if (buffer.readBoolean()) {
                int moduleIndex = buffer.readVarInt();
                if (moduleIndex < 0 || moduleIndex >= FactoryModule.values().length) {
                    throw new IllegalArgumentException("Unknown required module index " + moduleIndex);
                }
                module = FactoryModule.values()[moduleIndex];
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Invalid fluid stabilization result item");
            }
            return new FluidStabilizationRecipe(id, fluid, amount, result, module);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FluidStabilizationRecipe recipe) {
            buffer.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(recipe.inputFluid));
            buffer.writeVarInt(recipe.inputAmount);
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.requiredModule != null);
            if (recipe.requiredModule != null) {
                buffer.writeVarInt(recipe.requiredModule.ordinal());
            }
        }
    }
}
