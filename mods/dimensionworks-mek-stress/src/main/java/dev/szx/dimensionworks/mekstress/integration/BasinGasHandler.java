package dev.szx.dimensionworks.mekstress.integration;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.util.LazyOptional;

public final class BasinGasHandler implements IGasHandler {
    private static final long CAPACITY = 10_000L;
    private static final long LEAK_AMOUNT = 200L;
    private static final ResourceLocation BASIN_LID =
        new ResourceLocation("createdieselgenerators", "basin_lid");
    private static final String NBT_KEY = "DimensionWorksBasinGas";
    private static final String INPUT_NBT_KEY = "DimensionWorksBasinInputGas";

    private final BasinBlockEntity owner;
    private GasStack gas = GasStack.EMPTY;
    private GasStack inputGas = GasStack.EMPTY;
    private long lastLeakTick;
    private final LazyOptional<IGasHandler> capability = LazyOptional.of(() -> this);

    public BasinGasHandler(BasinBlockEntity owner) {
        this.owner = owner;
    }

    public LazyOptional<IGasHandler> capability() {
        return capability;
    }

    public void invalidate() {
        capability.invalidate();
    }

    public boolean acceptProducedGas(Gas producedGas, long amount, boolean simulate) {
        return processRecipeGas(null, 0L, producedGas, amount, simulate);
    }

    /** Validates both buffers before consuming input or storing a recipe output. */
    public boolean processRecipeGas(Gas requiredGas, long requiredAmount,
                                   Gas producedGas, long amount, boolean simulate) {
        if (amount <= 0 || amount > CAPACITY) {
            return false;
        }
        if (requiredAmount < 0 || (requiredGas == null && requiredAmount != 0)
            || (requiredGas != null && (requiredAmount <= 0 || inputGas.isEmpty()
                || !inputGas.getType().equals(requiredGas) || inputGas.getAmount() < requiredAmount))) {
            return false;
        }
        if (!gas.isEmpty() && !gas.getType().equals(producedGas)) {
            return false;
        }
        if (amount > CAPACITY - gas.getAmount()) {
            return false;
        }
        if (simulate) {
            return true;
        }

        if (requiredAmount > 0) {
            long remainingInput = inputGas.getAmount() - requiredAmount;
            inputGas = remainingInput == 0 ? GasStack.EMPTY : new GasStack(inputGas, remainingInput);
        }
        gas = new GasStack(producedGas, gas.getAmount() + amount);
        owner.setChanged();
        owner.notifyChangeOfContents();
        owner.notifyUpdate();
        return true;
    }

    public void leakIfDue() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long now = level.getGameTime();
        if (now - lastLeakTick < 20) {
            return;
        }
        lastLeakTick = now;

        // Advance the timer even when empty or sealed. Production must not
        // trigger extra leak events, and reopening must not accumulate losses.
        long totalGas = inputGas.getAmount() + gas.getAmount();
        if (totalGas == 0 || hasClosedLid(level)) {
            return;
        }

        // Keep the basin-wide loss at 200 mB, shared proportionally by its
        // input and output buffers rather than charging each buffer separately.
        long loss = Math.min(LEAK_AMOUNT, totalGas);
        long inputLoss = loss * inputGas.getAmount() / totalGas;
        long remainingInput = inputGas.getAmount() - inputLoss;
        inputGas = remainingInput == 0 ? GasStack.EMPTY : new GasStack(inputGas, remainingInput);
        long remaining = gas.getAmount() - (loss - inputLoss);
        gas = remaining == 0 ? GasStack.EMPTY : new GasStack(gas, remaining);
        owner.setChanged();
        owner.notifyChangeOfContents();
        owner.notifyUpdate();
    }

    private boolean hasClosedLid(Level level) {
        BlockState cover = level.getBlockState(owner.getBlockPos().above());
        return BASIN_LID.equals(BuiltInRegistries.BLOCK.getKey(cover.getBlock()))
            && cover.hasProperty(BlockStateProperties.OPEN)
            && !cover.getValue(BlockStateProperties.OPEN);
    }

    public void readFromNbt(CompoundTag tag) {
        gas = tag.contains(NBT_KEY, Tag.TAG_COMPOUND)
            ? GasStack.readFromNBT(tag.getCompound(NBT_KEY))
            : GasStack.EMPTY;
        inputGas = tag.contains(INPUT_NBT_KEY, Tag.TAG_COMPOUND)
            ? GasStack.readFromNBT(tag.getCompound(INPUT_NBT_KEY))
            : GasStack.EMPTY;
    }

    public void writeToNbt(CompoundTag tag) {
        tag.put(NBT_KEY, gas.write(new CompoundTag()));
        tag.put(INPUT_NBT_KEY, inputGas.write(new CompoundTag()));
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public GasStack getChemicalInTank(int tank) {
        return tank == 0 ? gas.copy() : tank == 1 ? inputGas.copy() : GasStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, GasStack stack) {
        if ((tank == 0 || tank == 1) && stack.getAmount() <= CAPACITY) {
            if (tank == 0) gas = stack.copy();
            else inputGas = stack.copy();
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        return tank == 0 || tank == 1 ? CAPACITY : 0;
    }

    @Override
    public boolean isValid(int tank, GasStack stack) {
        return tank == 1 && !stack.isEmpty();
    }

    @Override
    public GasStack insertChemical(int tank, GasStack stack, Action action) {
        if (!isValid(tank, stack)
            || (!inputGas.isEmpty() && !inputGas.getType().equals(stack.getType()))) {
            return stack;
        }
        long accepted = Math.min(stack.getAmount(), CAPACITY - inputGas.getAmount());
        if (accepted <= 0) return stack;
        if (action.execute()) {
            inputGas = new GasStack(stack.getType(), inputGas.getAmount() + accepted);
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
        long remainder = stack.getAmount() - accepted;
        return remainder == 0 ? GasStack.EMPTY : new GasStack(stack, remainder);
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action) {
        if (tank != 0 || amount <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }
        long extracted = Math.min(amount, gas.getAmount());
        GasStack result = new GasStack(gas, extracted);
        if (action.execute()) {
            long remaining = gas.getAmount() - extracted;
            gas = remaining == 0 ? GasStack.EMPTY : new GasStack(gas, remaining);
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
        return result;
    }
}
