package dev.szx.dimensionworks.mekstress.integration;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class BasinGasHandler implements IChemicalHandler {
    private static final long CAPACITY = 10_000L;
    private static final long LEAK_AMOUNT = 200L;
    private static final ResourceLocation BASIN_LID =
        ResourceLocation.fromNamespaceAndPath("createdieselgenerators", "basin_lid");
    private static final String NBT_KEY = "DimensionWorksBasinGas";
    private static final String INPUT_NBT_KEY = "DimensionWorksBasinInputGas";

    private final BasinBlockEntity owner;
    private ChemicalStack gas = ChemicalStack.EMPTY;
    private ChemicalStack inputGas = ChemicalStack.EMPTY;
    private long lastLeakTick;

    public BasinGasHandler(BasinBlockEntity owner) {
        this.owner = owner;
    }

    public boolean acceptProducedGas(Chemical producedGas, long amount, boolean simulate) {
        return processRecipeGas(null, 0L, producedGas, amount, simulate);
    }

    /** Validates both buffers before consuming input or storing an optional recipe output. */
    public boolean processRecipeGas(Chemical requiredGas, long requiredAmount,
                                   Chemical producedGas, long amount, boolean simulate) {
        boolean hasOutput = producedGas != null && amount > 0;
        if (!hasOutput && (producedGas != null || amount != 0)) {
            return false;
        }
        if (!hasOutput && requiredAmount <= 0) {
            return false;
        }
        if (requiredAmount < 0 || (requiredGas == null && requiredAmount != 0)
            || (requiredGas != null && (requiredAmount <= 0 || inputGas.isEmpty()
                || !inputGas.getChemical().equals(requiredGas) || inputGas.getAmount() < requiredAmount))) {
            return false;
        }
        if (hasOutput) {
            if (amount > CAPACITY || (!gas.isEmpty() && !gas.getChemical().equals(producedGas))
                || amount > CAPACITY - gas.getAmount()) {
                return false;
            }
        }
        if (simulate) {
            return true;
        }

        if (requiredAmount > 0) {
            long remainingInput = inputGas.getAmount() - requiredAmount;
            inputGas = remainingInput == 0 ? ChemicalStack.EMPTY : inputGas.copyWithAmount(remainingInput);
        }
        if (hasOutput) {
            gas = new ChemicalStack(producedGas, gas.getAmount() + amount);
        }
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
        inputGas = remainingInput == 0 ? ChemicalStack.EMPTY : inputGas.copyWithAmount(remainingInput);
        long remaining = gas.getAmount() - (loss - inputLoss);
        gas = remaining == 0 ? ChemicalStack.EMPTY : gas.copyWithAmount(remaining);
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

    public void readFromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        gas = tag.contains(NBT_KEY, Tag.TAG_COMPOUND)
            ? ChemicalStack.parseOptional(registries, tag.getCompound(NBT_KEY))
            : ChemicalStack.EMPTY;
        inputGas = tag.contains(INPUT_NBT_KEY, Tag.TAG_COMPOUND)
            ? ChemicalStack.parseOptional(registries, tag.getCompound(INPUT_NBT_KEY))
            : ChemicalStack.EMPTY;
    }

    public void writeToNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put(NBT_KEY, gas.saveOptional(registries));
        tag.put(INPUT_NBT_KEY, inputGas.saveOptional(registries));
    }

    @Override
    public int getChemicalTanks() {
        return 2;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return tank == 0 ? gas.copy() : tank == 1 ? inputGas.copy() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if ((tank == 0 || tank == 1) && stack.getAmount() <= CAPACITY) {
            if (tank == 0) gas = stack.copy();
            else inputGas = stack.copy();
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return tank == 0 || tank == 1 ? CAPACITY : 0;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return tank == 1 && !stack.isEmpty();
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (!isValid(tank, stack)
            || (!inputGas.isEmpty() && !inputGas.getChemical().equals(stack.getChemical()))) {
            return stack;
        }
        long accepted = Math.min(stack.getAmount(), CAPACITY - inputGas.getAmount());
        if (accepted <= 0) return stack;
        if (action.execute()) {
            inputGas = new ChemicalStack(stack.getChemical(), inputGas.getAmount() + accepted);
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
        long remainder = stack.getAmount() - accepted;
        return remainder == 0 ? ChemicalStack.EMPTY : stack.copyWithAmount(remainder);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        if (tank != 0 || amount <= 0 || gas.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        long extracted = Math.min(amount, gas.getAmount());
        ChemicalStack result = gas.copyWithAmount(extracted);
        if (action.execute()) {
            long remaining = gas.getAmount() - extracted;
            gas = remaining == 0 ? ChemicalStack.EMPTY : gas.copyWithAmount(remaining);
            owner.setChanged();
            owner.notifyChangeOfContents();
            owner.notifyUpdate();
        }
        return result;
    }
}
