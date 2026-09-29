package dev.szx.dimensionworks.cavefactory.blockentity;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.block.FactoryControllerBlock;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import dev.szx.dimensionworks.cavefactory.logic.StructureLayout;
import dev.szx.dimensionworks.cavefactory.recipe.CaveProcessingInput;
import dev.szx.dimensionworks.cavefactory.recipe.CaveProcessingRecipe;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import dev.szx.dimensionworks.cavefactory.registry.CFRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FactoryControllerBlockEntity extends FactoryMachineBlockEntity {
    private static final String VALID_KEY = "StructureValid";
    private static final String PROCESS_PROGRESS_KEY = "ProcessProgress";

    private final MachineType machineType;
    private boolean structureValid;
    private boolean structureDirty = true;
    private int processProgress;
    private RecipeManager cachedRecipeManager;
    private CaveProcessingRecipe cachedRecipe;
    private FactoryPhase cachedPhase;
    private FactoryModule cachedNumericModule;
    private FactoryModule cachedMechanismModule;
    private FluidStack cachedFluid = FluidStack.EMPTY;
    private final ItemStack[] cachedInputs = {
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private boolean recipeCacheValid;

    public FactoryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CFBlockEntities.CONTROLLER.get(), pos, state);
        this.machineType = state.getBlock() instanceof FactoryControllerBlock controller
            ? controller.machineType()
            : MachineType.ABYSSAL;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public MachineType machineType() {
        return machineType;
    }

    public boolean structureValid() {
        return structureValid;
    }

    public void markStructureDirty() {
        structureDirty = true;
    }

    @Override
    protected void onModuleSlotsChanged() {
        super.onModuleSlotsChanged();
        invalidateRecipeCache();
    }

    @Override
    protected void recordSuccessfulBatch() {
        super.recordSuccessfulBatch();
        invalidateRecipeCache();
    }

    @Override
    public void setPhaseState(FactoryPhase phase, int progress) {
        super.setPhaseState(phase, progress);
        invalidateRecipeCache();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (structureDirty && level != null && !level.isClientSide) {
            structureValid = validateStructure();
            structureDirty = false;
            setChanged();
            sendData();
        }
    }

    public boolean validateStructure() {
        if (level == null) {
            return false;
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (StructureLayout.isCenter(x, y, z)) {
                        continue;
                    }
                    BlockPos partPos = worldPosition.offset(x, y, z);
                    if (StructureLayout.isFaceCenter(x, y, z)) {
                        if (!level.getBlockState(partPos).is(CFBlocks.FACTORY_PORT.get())) {
                            return false;
                        }
                    } else if (!level.getBlockState(partPos).is(CFBlocks.FACTORY_CASING.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void processServerTick() {
        if (structureDirty) {
            structureValid = validateStructure();
            structureDirty = false;
        }
        if (!structureValid
            || !machineType.survivalEnabled()
            || !hasEffectiveSpeed()
            || level == null
            || !machineType.dimension().dimensionId().equals(level.dimension().location().toString())) {
            return;
        }

        CaveProcessingRecipe recipe = findRecipe();
        if (recipe == null) {
            processProgress = 0;
            return;
        }

        int budget = Math.max(1, batchesThisTick());
        while (budget > 0) {
            recipe = findRecipe();
            if (recipe == null) {
                processProgress = 0;
                setChanged();
                return;
            }
            int required = recipe.duration() - processProgress;
            int consumed = Math.min(budget, required);
            processProgress += consumed;
            budget -= consumed;
            if (processProgress < recipe.duration()) {
                break;
            }
            processProgress = 0;

            if (!canFitOutputs(recipe)) {
                consumeInputs(recipe);
                addResidueOrDrop(new ItemStack(CFItems.residue(recipe.residue()).get()));
                setChanged();
                return;
            }

            consumeInputs(recipe);
            for (ItemStack output : recipe.itemOutputs()) {
                insertOutput(output);
            }
            if (!recipe.fluidOutput().isEmpty()) {
                outputTank().fill(recipe.fluidOutput().copy(), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            moveFilledAmpoules();
            recordSuccessfulBatch();
        }
        setChanged();
    }

    @Nullable
    private CaveProcessingRecipe findRecipe() {
        if (level == null) {
            return null;
        }
        RecipeManager manager = level.getRecipeManager();
        FactoryPhase phase = phase();
        FactoryModule numericModule = moduleAt(0);
        FactoryModule mechanismModule = moduleAt(1);
        FluidStack fluid = inputTank().getFluid();
        if (recipeCacheValid
            && manager == cachedRecipeManager
            && phase == cachedPhase
            && numericModule == cachedNumericModule
            && mechanismModule == cachedMechanismModule
            && fluid.isFluidStackIdentical(cachedFluid)
            && inputsMatchCache()) {
            return cachedRecipe;
        }

        CaveProcessingInput input = new CaveProcessingInput(NonNullList.of(
            ItemStack.EMPTY,
            getStack(0), getStack(1), getStack(2), getStack(3)
        ), fluid);
        CaveProcessingRecipe match = null;
        for (CaveProcessingRecipe recipe : manager.getAllRecipesFor(CFRecipes.CAVE_PROCESSING_TYPE.get())) {
            if (recipe.matchesInventory(machineType, phase, numericModule, input)
                || recipe.matchesInventory(machineType, phase, mechanismModule, input)) {
                match = recipe;
                break;
            }
        }

        cachedRecipeManager = manager;
        cachedRecipe = match;
        cachedPhase = phase;
        cachedNumericModule = numericModule;
        cachedMechanismModule = mechanismModule;
        cachedFluid = fluid.copy();
        for (int slot = 0; slot < cachedInputs.length; slot++) {
            cachedInputs[slot] = getStack(slot).copy();
        }
        recipeCacheValid = true;
        return match;
    }

    private boolean inputsMatchCache() {
        for (int slot = 0; slot < cachedInputs.length; slot++) {
            if (!ItemStack.matches(cachedInputs[slot], getStack(slot))) {
                return false;
            }
        }
        return true;
    }

    private void invalidateRecipeCache() {
        recipeCacheValid = false;
    }

    private boolean canFitOutputs(CaveProcessingRecipe recipe) {
        ItemStack[] simulated = new ItemStack[outputItems().getSlots()];
        for (int slot = 0; slot < simulated.length; slot++) {
            simulated[slot] = outputItems().getStackInSlot(slot).copy();
        }
        for (ItemStack output : recipe.itemOutputs()) {
            ItemStack remaining = output.copy();
            for (int slot = 0; slot < simulated.length && !remaining.isEmpty(); slot++) {
                ItemStack existing = simulated[slot];
                if (existing.isEmpty()) {
                    simulated[slot] = remaining.copy();
                    remaining = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameTags(existing, remaining)) {
                    int room = existing.getMaxStackSize() - existing.getCount();
                    int moved = Math.min(room, remaining.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        FluidStack fluidOutput = recipe.fluidOutput();
        if (!fluidOutput.isEmpty()) {
            FluidStack current = outputTank().getFluid();
            if (!current.isEmpty() && !current.isFluidEqual(fluidOutput)) {
                return false;
            }
            int amount = current.isEmpty() ? 0 : current.getAmount();
            if (amount + fluidOutput.getAmount() > outputTank().getCapacity()) {
                return false;
            }
        }
        return true;
    }

    private void insertOutput(ItemStack output) {
        ItemStack remaining = insertInternal(outputItems(), output, false);
        if (!remaining.isEmpty()) {
            addOrDrop(remaining);
        }
    }

    private void consumeInputs(CaveProcessingRecipe recipe) {
        for (CaveProcessingRecipe.IngredientStack requirement : recipe.ingredients()) {
            int remaining = requirement.count();
            for (int slot = 0; slot < inputItems().getSlots() && remaining > 0; slot++) {
                ItemStack stack = inputItems().getStackInSlot(slot);
                if (!requirement.test(stack)) {
                    continue;
                }
                int used = Math.min(remaining, stack.getCount());
                inputItems().extractItem(slot, used, false);
                remaining -= used;
            }
        }
        if (!recipe.fluidInput().isEmpty()) {
            inputTank().drain(recipe.fluidInput().getAmount(),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    private void moveFilledAmpoules() {
        ItemStack empty = ampouleInput().getStackInSlot(0);
        if (empty.isEmpty() || !(empty.getItem() instanceof dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem)) {
            return;
        }
        FluidStack available = outputTank().getFluid();
        if (available.isEmpty()) {
            return;
        }
        ItemStack filled = empty.copyWithCount(1);
        int toMove = Math.min(dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem.capacity(), available.getAmount());
        FluidStack drain = outputTank().drain(toMove,
            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        if (drain.isEmpty()) {
            return;
        }
        filled.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
            .ifPresent(handler -> handler.fill(drain, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        filled.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
            .ifPresent(handler -> dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem.setOrigin(filled, machineType.dimension()));
        if (insertInternal(ampouleOutput(), 0, filled, true).isEmpty()) {
            ampouleInput().extractItem(0, 1, false);
            insertInternal(ampouleOutput(), 0, filled, false);
            outputTank().drain(drain.getAmount(),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public MachineCapability machineCapability() {
        return MachineCapability.ITEM_AND_FLUID_TANKS;
    }

    @Override
    public MachineType machineTypeForResidue() {
        return machineType;
    }

    @Override
    public float calculateStressApplied() {
        float impact = stressImpactWithOverspeed(CaveFactoryConfig.STRESS_IMPACT.get().floatValue());
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean(VALID_KEY, structureValid);
        tag.putInt(PROCESS_PROGRESS_KEY, processProgress);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        structureValid = tag.getBoolean(VALID_KEY);
        processProgress = tag.getInt(PROCESS_PROGRESS_KEY);
        structureDirty = true;
        invalidateRecipeCache();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimensionworks_cave_factory." + machineType.id());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        structureDirty = true;
        invalidateRecipeCache();
    }

    @Override
    public void destroy() {
        if (inputTank().getFluidAmount() > 0 || outputTank().getFluidAmount() > 0) {
            if (level != null && !level.isClientSide) {
                int count = Math.max(1, (inputTank().getFluidAmount() + outputTank().getFluidAmount()) / 1_000);
                level.addFreshEntity(new ItemEntity(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    new ItemStack(CFItems.residue(machineType).get(), count)
                ));
            }
            inputTank().setFluid(FluidStack.EMPTY);
            outputTank().setFluid(FluidStack.EMPTY);
        }
        super.destroy();
    }
}
