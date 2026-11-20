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
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import net.minecraft.core.NonNullList;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidStabilizationRecipe implements Recipe<CaveProcessingInput> {
    private final net.minecraft.world.level.material.Fluid inputFluid;
    private final int inputAmount;
    private final ItemStack result;
    private final FactoryModule requiredModule;

    public FluidStabilizationRecipe(
        net.minecraft.world.level.material.Fluid inputFluid,
        int inputAmount,
        ItemStack result,
        FactoryModule requiredModule
    ) {
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
    public ItemStack assemble(CaveProcessingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
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
        private record Input(net.minecraft.core.Holder<net.minecraft.world.level.material.Fluid> fluid, Optional<Integer> amount) {}
        private static final Codec<Input> INPUT = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.FLUID_NON_EMPTY_CODEC.validate(holder -> CFFluids.isFactoryFluid(new FluidStack(holder, 1))
                ? com.mojang.serialization.DataResult.success(holder)
                : com.mojang.serialization.DataResult.error(() -> "Expected factory fluid"))
                .fieldOf("id").forGetter(Input::fluid),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("amount").forGetter(Input::amount)
        ).apply(instance, Input::new));
        private static final MapCodec<FluidStabilizationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            INPUT.fieldOf("input").forGetter(r -> new Input(r.inputFluid.builtInRegistryHolder(), Optional.of(r.inputAmount))),
            ItemStack.CODEC.fieldOf("result").forGetter(FluidStabilizationRecipe::result),
            FactoryRecipeCodecs.MODULE.optionalFieldOf("required_module").forGetter(r -> Optional.ofNullable(r.requiredModule))
        ).apply(instance, (input, result, module) -> new FluidStabilizationRecipe(
            input.fluid().value(), input.amount().orElseGet(dev.szx.dimensionworks.cavefactory.CaveFactoryConfig::stabilizationInput), result, module.orElse(null))));
        private static final StreamCodec<RegistryFriendlyByteBuf, FluidStabilizationRecipe> STREAM =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<FluidStabilizationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FluidStabilizationRecipe> streamCodec() { return STREAM; }
    }
}
