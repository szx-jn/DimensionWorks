package dev.szx.dimensionworks.cavefactory.blockentity;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.recipe.FluidStabilizationRecipe;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import dev.szx.dimensionworks.cavefactory.registry.CFRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidStabilizerBlockEntity extends FactoryMachineBlockEntity {
    private final LazyOptional<IFluidHandler> publicFluidCapability = LazyOptional.of(this::inputTank);
    private final LazyOptional<IItemHandler> publicItemOutputCapability = LazyOptional.of(this::outputItems);
    private RecipeManager cachedRecipeManager;
    private FluidStabilizationRecipe cachedRecipe;
    private FactoryModule cachedNumericModule;
    private FactoryModule cachedMechanismModule;
    private FluidStack cachedFluid = FluidStack.EMPTY;
    private boolean recipeCacheValid;

    public FluidStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(CFBlockEntities.STABILIZER.get(), pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return publicFluidCapability.cast();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return publicItemOutputCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        publicFluidCapability.invalidate();
        publicItemOutputCapability.invalidate();
        super.invalidateCaps();
    }

    @Override
    protected void onModuleSlotsChanged() {
        super.onModuleSlotsChanged();
        recipeCacheValid = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        recipeCacheValid = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    protected void processServerTick() {
        if (!hasEffectiveSpeed() || level == null || inputTank().isEmpty()) {
            return;
        }
        FluidStabilizationRecipe recipe = findRecipe();
        if (recipe == null) {
            return;
        }
        int availableOperations = dev.szx.dimensionworks.cavefactory.logic.StabilizationMath.evaluate(
            inputTank().getFluidAmount(),
            recipe.inputAmount()
        ).outputs();
        int operations = Math.min(Math.max(1, batchesThisTick()), availableOperations);
        for (int i = 0; i < operations; i++) {
            ItemStack result = recipe.result();
            if (!canAccept(result)) {
                return;
            }
            inputTank().drain(recipe.inputAmount(), IFluidHandler.FluidAction.EXECUTE);
            insertResult(result);
            recordSuccessfulBatch();
        }
    }

    private FluidStabilizationRecipe findRecipe() {
        if (level == null) {
            return null;
        }
        RecipeManager manager = level.getRecipeManager();
        FactoryModule numericModule = moduleAt(0);
        FactoryModule mechanismModule = moduleAt(1);
        FluidStack fluid = inputTank().getFluid();
        if (recipeCacheValid
            && manager == cachedRecipeManager
            && numericModule == cachedNumericModule
            && mechanismModule == cachedMechanismModule
            && fluid.isFluidStackIdentical(cachedFluid)) {
            return cachedRecipe;
        }

        FluidStabilizationRecipe match = null;
        for (FluidStabilizationRecipe recipe : manager.getAllRecipesFor(CFRecipes.FLUID_STABILIZATION_TYPE.get())) {
            if (recipe.matches(fluid, numericModule) || recipe.matches(fluid, mechanismModule)) {
                match = recipe;
                break;
            }
        }
        cachedRecipeManager = manager;
        cachedRecipe = match;
        cachedNumericModule = numericModule;
        cachedMechanismModule = mechanismModule;
        cachedFluid = fluid.copy();
        recipeCacheValid = true;
        return match;
    }

    private boolean canAccept(ItemStack result) {
        ItemStack simulated = insertInternal(outputItems(), result, true);
        return simulated.isEmpty();
    }

    private void insertResult(ItemStack result) {
        ItemStack remaining = insertInternal(outputItems(), result, false);
        if (!remaining.isEmpty()) {
            addOrDrop(remaining);
        }
    }

    @Override
    public MachineCapability machineCapability() {
        return MachineCapability.ITEM_AND_FLUID_TANKS;
    }

    @Override
    public MachineType machineTypeForResidue() {
        if (inputTank().isEmpty()) {
            return MachineType.ABYSSAL;
        }
        for (MachineType machine : MachineType.values()) {
            var entry = dev.szx.dimensionworks.cavefactory.registry.CFFluids.get(machine);
            if (entry != null && (inputTank().getFluid().getFluid() == entry.still().get()
                || inputTank().getFluid().getFluid() == entry.flowing().get())) {
                return machine;
            }
        }
        return MachineType.ABYSSAL;
    }

    @Override
    public float calculateStressApplied() {
        float impact = stressImpactWithOverspeed(CaveFactoryConfig.STRESS_IMPACT.get().floatValue());
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimensionworks_cave_factory.fluid_stabilizer");
    }
}
