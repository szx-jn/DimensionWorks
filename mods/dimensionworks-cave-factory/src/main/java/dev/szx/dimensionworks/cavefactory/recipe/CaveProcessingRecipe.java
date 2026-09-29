package dev.szx.dimensionworks.cavefactory.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
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

import java.util.ArrayList;
import java.util.List;

public final class CaveProcessingRecipe implements Recipe<CaveProcessingInput> {
    public record IngredientStack(Ingredient ingredient, int count) {
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }

    private final ResourceLocation id;
    private final MachineType machine;
    private final FactoryPhase phase;
    private final List<IngredientStack> ingredients;
    private final FluidStack fluidInput;
    private final List<ItemStack> itemOutputs;
    private final FluidStack fluidOutput;
    private final int duration;
    private final FactoryModule requiredModule;
    private final MachineType residue;

    public CaveProcessingRecipe(
        ResourceLocation id,
        MachineType machine,
        FactoryPhase phase,
        List<IngredientStack> ingredients,
        FluidStack fluidInput,
        List<ItemStack> itemOutputs,
        FluidStack fluidOutput,
        int duration,
        FactoryModule requiredModule,
        MachineType residue
    ) {
        this.id = id;
        this.machine = machine;
        this.phase = phase;
        this.ingredients = List.copyOf(ingredients);
        this.fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput.copy();
        this.itemOutputs = itemOutputs.stream().map(ItemStack::copy).toList();
        this.fluidOutput = fluidOutput == null ? FluidStack.EMPTY : fluidOutput.copy();
        this.duration = Math.max(1, duration);
        this.requiredModule = requiredModule;
        this.residue = residue;
    }

    public MachineType machine() {
        return machine;
    }

    public FactoryPhase phase() {
        return phase;
    }

    public List<IngredientStack> ingredients() {
        return ingredients;
    }

    public FluidStack fluidInput() {
        return fluidInput;
    }

    public List<ItemStack> itemOutputs() {
        return itemOutputs;
    }

    public FluidStack fluidOutput() {
        return fluidOutput;
    }

    public int duration() {
        return duration;
    }

    public FactoryModule requiredModule() {
        return requiredModule;
    }

    public MachineType residue() {
        return residue;
    }

    public boolean matchesMachine(MachineType candidate, FactoryPhase candidatePhase, FactoryModule installed) {
        return machine == candidate
            && phase == candidatePhase
            && (requiredModule == null || requiredModule == installed)
            && matchesInputs(null);
    }

    public boolean matchesInventory(
        MachineType candidate,
        FactoryPhase candidatePhase,
        FactoryModule installed,
        CaveProcessingInput input
    ) {
        return machine == candidate
            && phase == candidatePhase
            && (requiredModule == null || requiredModule == installed)
            && matches(input, null);
    }

    @Override
    public boolean matches(CaveProcessingInput input, Level level) {
        if (fluidInput.getAmount() > 0
            && (input.fluidInput().isEmpty()
                || !fluidInput.isFluidEqual(input.fluidInput())
                || input.fluidInput().getAmount() < fluidInput.getAmount())) {
            return false;
        }
        return matchesInputs(input);
    }

    private boolean matchesInputs(CaveProcessingInput input) {
        int[] available = new int[input == null ? 0 : input.getContainerSize()];
        if (input != null) {
            for (int slot = 0; slot < input.getContainerSize(); slot++) {
                available[slot] = input.getItem(slot).getCount();
            }
        }
        for (IngredientStack requirement : ingredients) {
            if (input == null) {
                if (!(requirement.ingredient().getItems().length > 0)) {
                    return false;
                }
                continue;
            }
            int remaining = requirement.count();
            for (int slot = 0; slot < input.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = input.getItem(slot);
                if (requirement.test(stack)) {
                    int used = Math.min(remaining, available[slot]);
                    available[slot] -= used;
                    remaining -= used;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CaveProcessingInput input, RegistryAccess registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return itemOutputs.isEmpty() ? ItemStack.EMPTY : itemOutputs.get(0).copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        for (IngredientStack stack : ingredients) {
            result.add(stack.ingredient());
        }
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.CAVE_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.CAVE_PROCESSING_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<CaveProcessingRecipe> {
        @Override
        public CaveProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            MachineType machine = MachineType.fromId(GsonHelper.getAsString(json, "machine"));
            if (machine == null) {
                throw new IllegalArgumentException("Unknown machine " + GsonHelper.getAsString(json, "machine"));
            }
            FactoryPhase phase = parsePhase(GsonHelper.getAsString(json, "phase", "phase_a"));
            List<IngredientStack> ingredients = parseIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
            FluidStack fluidInput = parseFluid(json.getAsJsonObject("fluid_input"));
            List<ItemStack> outputs = parseItems(GsonHelper.getAsJsonArray(json, "results"));
            FluidStack fluidOutput = parseFluid(json.getAsJsonObject("fluid_output"));
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            FactoryModule module = parseModule(json);
            MachineType residue = json.has("residue")
                ? MachineType.fromId(GsonHelper.getAsString(json, "residue"))
                : machine;
            return new CaveProcessingRecipe(
                id, machine, phase, ingredients, fluidInput, outputs, fluidOutput, duration, module, residue
            );
        }

        @Override
        public CaveProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            MachineType machine = MachineType.values()[buffer.readVarInt()];
            FactoryPhase phase = buffer.readEnum(FactoryPhase.class);
            int ingredientCount = buffer.readVarInt();
            List<IngredientStack> ingredients = new ArrayList<>(ingredientCount);
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(new IngredientStack(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            FluidStack fluidInput = FluidStack.readFromPacket(buffer);
            int outputCount = buffer.readVarInt();
            List<ItemStack> outputs = new ArrayList<>(outputCount);
            for (int i = 0; i < outputCount; i++) {
                outputs.add(buffer.readItem());
            }
            FluidStack fluidOutput = FluidStack.readFromPacket(buffer);
            int duration = buffer.readVarInt();
            FactoryModule module = readModule(buffer);
            MachineType residue = buffer.readBoolean() ? MachineType.values()[buffer.readVarInt()] : null;
            return new CaveProcessingRecipe(
                id, machine, phase, ingredients, fluidInput, outputs, fluidOutput, duration, module, residue
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CaveProcessingRecipe recipe) {
            buffer.writeVarInt(recipe.machine.ordinal());
            buffer.writeEnum(recipe.phase);
            buffer.writeVarInt(recipe.ingredients.size());
            for (IngredientStack stack : recipe.ingredients) {
                stack.ingredient().toNetwork(buffer);
                buffer.writeVarInt(stack.count());
            }
            recipe.fluidInput.writeToPacket(buffer);
            buffer.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack output : recipe.itemOutputs) {
                buffer.writeItem(output);
            }
            recipe.fluidOutput.writeToPacket(buffer);
            buffer.writeVarInt(recipe.duration);
            writeModule(buffer, recipe.requiredModule);
            buffer.writeBoolean(recipe.residue != null);
            if (recipe.residue != null) {
                buffer.writeVarInt(recipe.residue.ordinal());
            }
        }

        private static FactoryPhase parsePhase(String value) {
            return switch (value.toLowerCase()) {
                case "phase_a", "a", "primary" -> FactoryPhase.PHASE_A;
                case "phase_b", "b", "variant" -> FactoryPhase.PHASE_B;
                default -> throw new IllegalArgumentException("Unknown phase " + value);
            };
        }

        private static List<IngredientStack> parseIngredients(JsonArray array) {
            List<IngredientStack> ingredients = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                JsonElement ingredientJson = object.has("ingredient") ? object.get("ingredient") : object;
                int count = object.has("count") ? GsonHelper.getAsInt(object, "count", 1) : 1;
                ingredients.add(new IngredientStack(Ingredient.fromJson(ingredientJson), Math.max(1, count)));
            }
            return ingredients;
        }

        private static List<ItemStack> parseItems(JsonArray array) {
            List<ItemStack> stacks = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(object, "item"));
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(itemId));
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Unknown output item " + itemId);
                }
                stack.setCount(Math.max(1, GsonHelper.getAsInt(object, "count", 1)));
                stacks.add(stack);
            }
            return stacks;
        }

        private static FluidStack parseFluid(JsonObject object) {
            if (object == null) {
                return FluidStack.EMPTY;
            }
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(object, "fluid"));
            net.minecraft.world.level.material.Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) {
                throw new IllegalArgumentException("Unknown fluid " + fluidId);
            }
            return new FluidStack(fluid, Math.max(1, GsonHelper.getAsInt(object, "amount", 1)));
        }

        private static FactoryModule parseModule(JsonObject json) {
            String module = GsonHelper.getAsString(json, "required_module", "");
            if (module.isBlank()) {
                return null;
            }
            for (FactoryModule candidate : FactoryModule.values()) {
                if (candidate.id().equals(module) || candidate.name().equalsIgnoreCase(module)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown module " + module);
        }

        private static FactoryModule readModule(FriendlyByteBuf buffer) {
            return buffer.readBoolean() ? FactoryModule.values()[buffer.readVarInt()] : null;
        }

        private static void writeModule(FriendlyByteBuf buffer, FactoryModule module) {
            buffer.writeBoolean(module != null);
            if (module != null) {
                buffer.writeVarInt(module.ordinal());
            }
        }
    }
}
