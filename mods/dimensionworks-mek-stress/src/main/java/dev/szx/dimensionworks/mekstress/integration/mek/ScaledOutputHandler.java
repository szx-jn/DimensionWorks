package dev.szx.dimensionworks.mekstress.integration.mek;

import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.outputs.IOutputHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Applies the mechanical production multiplier at the logical output boundary.
 *
 * <p>Mekanism's output handlers scale an operation count before splitting composite
 * outputs. Wrapping the handler keeps all products on the same operation count and
 * avoids multiplying chance-based secondary outputs once per loop iteration.
 */
public final class ScaledOutputHandler<T> implements IOutputHandler<T> {
    private final IOutputHandler<T> delegate;
    private final OutputWriter<T> writer;
    private final CapacityWriter<T> capacityWriter;

    private ScaledOutputHandler(IOutputHandler<T> delegate, OutputWriter<T> writer, CapacityWriter<T> capacityWriter) {
        this.delegate = delegate;
        this.writer = writer;
        this.capacityWriter = capacityWriter;
    }

    @Override
    public void handleOutput(T toOutput, int operations) {
        if (!MachineWorkContext.scaling()) {
            delegate.handleOutput(toOutput, operations);
        } else {
            writer.write(toOutput, operations);
        }
    }

    @Override
    public void calculateOperationsCanSupport(OperationTracker tracker, T toOutput) {
        if (!MachineWorkContext.scaling()) {
            delegate.calculateOperationsCanSupport(tracker, toOutput);
        } else {
            capacityWriter.update(tracker, toOutput);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <STACK extends ChemicalStack<?>> IOutputHandler<STACK> wrapChemical(
        IOutputHandler<STACK> delegate, IChemicalTank<?, STACK> tank, RecipeError error) {
        IChemicalTank rawTank = tank;
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> MachineOutputDelivery.insertChemical(rawTank, output, operations),
            (tracker, output) -> MachineOutputDelivery.capChemical(tracker, error, rawTank, output));
    }

    public static IOutputHandler<FluidStack> wrapFluid(IExtendedFluidTank tank, RecipeError error,
                                                       IOutputHandler<FluidStack> delegate) {
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> MachineOutputDelivery.insertFluid(tank, output, operations),
            (tracker, output) -> MachineOutputDelivery.capFluid(tracker, error, tank, output));
    }

    public static IOutputHandler<ItemStack> wrapItem(IInventorySlot slot, RecipeError error,
                                                     IOutputHandler<ItemStack> delegate) {
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> MachineOutputDelivery.insertItem(slot, output, operations),
            (tracker, output) -> MachineOutputDelivery.capItem(tracker, error, slot, output));
    }

    public static IOutputHandler<SawmillRecipe.ChanceOutput> wrapChance(
        IInventorySlot mainSlot, RecipeError mainError, IInventorySlot secondarySlot, RecipeError secondaryError,
        IOutputHandler<SawmillRecipe.ChanceOutput> delegate) {
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> handleChanceOutput(mainSlot, secondarySlot, output, operations),
            (tracker, output) -> {
                MachineOutputDelivery.capItem(tracker, mainError, mainSlot, output.getMainOutput());
                if (tracker.shouldContinueChecking()) {
                    MachineOutputDelivery.capItem(tracker, secondaryError, secondarySlot,
                        output.getMaxSecondaryOutput());
                }
            });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static IOutputHandler<PressurizedReactionRecipe.PressurizedReactionRecipeOutput> wrapReaction(
        IInventorySlot slot, RecipeError slotError, IGasTank tank, RecipeError tankError,
        IOutputHandler<PressurizedReactionRecipe.PressurizedReactionRecipeOutput> delegate) {
        IGasTank rawTank = tank;
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> {
                MachineOutputDelivery.insertItem(slot, output.item(), operations);
                MachineOutputDelivery.insertChemical(rawTank, output.gas(), operations);
            },
            (tracker, output) -> {
                MachineOutputDelivery.capItem(tracker, slotError, slot, output.item());
                if (tracker.shouldContinueChecking()) {
                    MachineOutputDelivery.capChemical(tracker, tankError, rawTank, output.gas());
                }
            });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static IOutputHandler<ElectrolysisRecipe.ElectrolysisRecipeOutput> wrapElectrolysis(
        IGasTank leftTank, RecipeError leftError, IGasTank rightTank, RecipeError rightError,
        IOutputHandler<ElectrolysisRecipe.ElectrolysisRecipeOutput> delegate) {
        IGasTank rawLeftTank = leftTank;
        IGasTank rawRightTank = rightTank;
        return new ScaledOutputHandler<>(delegate,
            (output, operations) -> {
                MachineOutputDelivery.insertChemical(rawLeftTank, output.left(), operations);
                MachineOutputDelivery.insertChemical(rawRightTank, output.right(), operations);
            },
            (tracker, output) -> {
                MachineOutputDelivery.capChemical(tracker, leftError, rawLeftTank, output.left());
                if (tracker.shouldContinueChecking()) {
                    MachineOutputDelivery.capChemical(tracker, rightError, rawRightTank, output.right());
                }
            });
    }

    private static void handleChanceOutput(IInventorySlot mainSlot, IInventorySlot secondarySlot,
                                           SawmillRecipe.ChanceOutput output, int operations) {
        MachineOutputDelivery.insertItem(mainSlot, output.getMainOutput(), operations);

        List<AccumulatedItem> secondaryOutputs = new ArrayList<>();
        ItemStack secondaryOutput = output.getSecondaryOutput();
        for (int i = 0; i < operations; i++) {
            accumulate(secondaryOutputs, secondaryOutput);
            if (i < operations - 1) {
                secondaryOutput = output.nextSecondaryOutput();
            }
        }
        for (AccumulatedItem accumulated : secondaryOutputs) {
            MachineOutputDelivery.insertItemScaled(secondarySlot, accumulated.stack, accumulated.count, 1);
        }
    }

    private static void accumulate(List<AccumulatedItem> outputs, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (AccumulatedItem output : outputs) {
            if (ItemStack.isSameItemSameTags(output.stack, stack)) {
                output.count += stack.getCount();
                return;
            }
        }
        outputs.add(new AccumulatedItem(stack.copy(), stack.getCount()));
    }

    @FunctionalInterface
    private interface OutputWriter<T> {
        void write(T output, int operations);
    }

    @FunctionalInterface
    private interface CapacityWriter<T> {
        void update(OperationTracker tracker, T output);
    }

    private static final class AccumulatedItem {
        private final ItemStack stack;
        private long count;

        private AccumulatedItem(ItemStack stack, long count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
