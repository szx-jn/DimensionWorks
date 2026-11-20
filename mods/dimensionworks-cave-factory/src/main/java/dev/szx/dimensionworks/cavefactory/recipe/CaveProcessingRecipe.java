package dev.szx.dimensionworks.cavefactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.Optional;

import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import net.minecraft.core.NonNullList;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class CaveProcessingRecipe implements Recipe<CaveProcessingInput> {
    public record IngredientStack(Ingredient ingredient, int count) {
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }

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
                || !FluidStack.isSameFluidSameComponents(fluidInput, input.fluidInput())
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
    public ItemStack assemble(CaveProcessingInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
    public RecipeSerializer<?> getSerializer() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.CAVE_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return dev.szx.dimensionworks.cavefactory.registry.CFRecipes.CAVE_PROCESSING_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<CaveProcessingRecipe> {
        private static final Codec<IngredientStack> INGREDIENT = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(IngredientStack::ingredient),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(IngredientStack::count)
        ).apply(instance, IngredientStack::new));
        private static final MapCodec<CaveProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FactoryRecipeCodecs.MACHINE.fieldOf("machine").forGetter(CaveProcessingRecipe::machine),
            FactoryRecipeCodecs.PHASE.optionalFieldOf("phase", FactoryPhase.PHASE_A).forGetter(CaveProcessingRecipe::phase),
            INGREDIENT.listOf().fieldOf("ingredients").forGetter(CaveProcessingRecipe::ingredients),
            FluidStack.CODEC.optionalFieldOf("fluid_input", FluidStack.EMPTY).forGetter(CaveProcessingRecipe::fluidInput),
            ItemStack.CODEC.listOf().fieldOf("results").forGetter(CaveProcessingRecipe::itemOutputs),
            FluidStack.CODEC.optionalFieldOf("fluid_output", FluidStack.EMPTY).forGetter(CaveProcessingRecipe::fluidOutput),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 100).forGetter(CaveProcessingRecipe::duration),
            FactoryRecipeCodecs.MODULE.optionalFieldOf("required_module").forGetter(r -> Optional.ofNullable(r.requiredModule)),
            FactoryRecipeCodecs.MACHINE.optionalFieldOf("residue").forGetter(r -> Optional.ofNullable(r.residue))
        ).apply(instance, (machine, phase, ingredients, fluidInput, outputs, fluidOutput, duration, module, residue) ->
            new CaveProcessingRecipe(machine, phase, ingredients, fluidInput, outputs, fluidOutput, duration,
                module.orElse(null), residue.orElse(machine))));
        private static final StreamCodec<RegistryFriendlyByteBuf, CaveProcessingRecipe> STREAM =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<CaveProcessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CaveProcessingRecipe> streamCodec() { return STREAM; }
    }
}
