package dev.szx.dimensionworks.cavefactory.blockentity;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.recipe.FluidStabilizationRecipe;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import dev.szx.dimensionworks.cavefactory.registry.CFRecipes;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartOwnerResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidStabilizerBlockEntity extends FactoryMachineBlockEntity {
    private long cachedRecipeGeneration = -1;
    private RecipeManager cachedRecipeManager;
    private FluidStabilizationRecipe cachedRecipe;
    private FactoryModule cachedNumericModule;
    private FactoryModule cachedMechanismModule;
    private FluidStack cachedFluid = FluidStack.EMPTY;
    private boolean recipeCacheValid;

    public FluidStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(CFBlockEntities.STABILIZER.get(), pos, state);
    }

    @Override public IFluidHandler fluidHandler(@Nullable Direction side) { return isRemoved() ? null : inputTank(); }
    @Override public IItemHandler itemHandler(@Nullable Direction side) { return isRemoved() ? null : outputItems(); }

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
        ItemStack result = recipe.result();
        operations = (int) Math.min(operations, outputRoom(result) / result.getCount());
        if (operations == 0) return;
        long gateAmount = (long) result.getCount() * operations;
        if (!canAccept(result)
            || !FinalProductionGate.tryConsumeItem(
                GearHeartOwnerResolver.normalize(owner()), result, gateAmount,
                level.getGameTime())) {
            return;
        }
        for (int i = 0; i < operations; i++) {
            inputTank().drain(recipe.inputAmount(), IFluidHandler.FluidAction.EXECUTE);
            insertResult(result);
            recordSuccessfulBatch();
        }
    }

    private long outputRoom(ItemStack result) {
        long room = 0;
        for (int slot = 0; slot < outputItems().getSlots(); slot++) {
            ItemStack present = outputItems().getStackInSlot(slot);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, result)) continue;
            room += Math.max(0, Math.min(outputItems().getSlotLimit(slot), result.getMaxStackSize()) - present.getCount());
        }
        return room;
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
            && cachedRecipeGeneration == dev.szx.dimensionworks.cavefactory.logic.FactoryRecipeReload.generation()
            && numericModule == cachedNumericModule
            && mechanismModule == cachedMechanismModule
            && FluidStack.matches(fluid, cachedFluid)) {
            return cachedRecipe;
        }

        FluidStabilizationRecipe match = null;
        for (var holder : manager.getAllRecipesFor(CFRecipes.FLUID_STABILIZATION_TYPE.get())) {
            FluidStabilizationRecipe recipe = holder.value();
            if (recipe.matches(fluid, numericModule) || recipe.matches(fluid, mechanismModule)) {
                match = recipe;
                break;
            }
        }
        cachedRecipeManager = manager;
        cachedRecipeGeneration = dev.szx.dimensionworks.cavefactory.logic.FactoryRecipeReload.generation();
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
