package dev.szx.dimensionworks.mekstress.integration.mek;

import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartOwnerResolver;
import dev.szx.dimensionworks.rpmlimit.quota.MekProcessingContext;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.UUID;

/** Shared output insertion and capacity checks for the mechanical production multiplier. */
public final class MachineOutputDelivery {
    private MachineOutputDelivery() {
    }

    public static void insertItem(IInventorySlot slot, ItemStack toOutput, int operations) {
        if (!MachineWorkContext.scaling() || operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        insertItemScaled(slot, toOutput, toOutput.getCount(), operations);
    }

    public static void insertItemScaled(IInventorySlot slot, ItemStack output, long base, int operations) {
        if (output.isEmpty() || base <= 0 || operations <= 0) return;
        long count = ItemOutputCredit.preview(slot, output, base, operations);
        if (count > Integer.MAX_VALUE || !slot.insertItem(output.copyWithCount((int) count),
            Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) return;
        if (insertItemAmount(slot, output, count)) {
            ItemOutputCredit.commit(slot, output, base, operations);
        }
    }

    public static boolean insertItemAmount(IInventorySlot slot, ItemStack toOutput, long count) {
        if (count <= 0L || toOutput.isEmpty()) {
            return false;
        }
        int batchSize = Math.max(1, toOutput.getMaxStackSize());
        long remaining = count;
        while (remaining > 0L) {
            int batchCount = (int) Math.min((long) batchSize, remaining);
            ItemStack remainder = slot.insertItem(toOutput.copyWithCount(batchCount),
                Action.EXECUTE, AutomationType.INTERNAL);
            long inserted = batchCount - (long) remainder.getCount();
            if (inserted <= 0L) {
                return remaining < count;
            }
            remaining -= inserted;
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void insertChemical(IChemicalTank tank, ChemicalStack toOutput, int operations) {
        if (!MachineWorkContext.scaling() || operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        long amount = MachineWorkContext.scaleOutput(toOutput.getAmount(), operations);
        if (amount > 0L) {
            tank.insert(tank.createStack(toOutput, amount), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    public static void insertFluid(IExtendedFluidTank tank, FluidStack toOutput, int operations) {
        if (!MachineWorkContext.scaling() || operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        long amount = MachineWorkContext.scaleOutput(toOutput.getAmount(), operations);
        if (amount > 0L) {
            tank.insert(new FluidStack(toOutput, (int) Math.min(Integer.MAX_VALUE, amount)),
                Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    public static void capItem(OperationTracker tracker, RecipeError notEnoughSpace, IInventorySlot slot,
                               ItemStack toOutput) {
        if (toOutput.isEmpty()) {
            return;
        }
        ItemStack maxOutput = toOutput.copyWithCount(toOutput.getMaxStackSize());
        ItemStack remainder = slot.insertItem(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getCount() - remainder.getCount();
        int operations = ItemOutputCredit.maxOperations(slot, toOutput, availableRoom);
        if (operations > 0 && !allowFinalProduction(
            toOutput, ItemOutputCredit.preview(slot, toOutput, toOutput.getCount(), operations))) {
            operations = 0;
        }
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && slot.getLimit(slot.getStack()) - slot.getCount() > 0) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void capChemical(OperationTracker tracker, RecipeError notEnoughSpace, IChemicalTank tank,
                                   ChemicalStack toOutput) {
        if (toOutput.isEmpty()) {
            return;
        }
        ChemicalStack maxOutput = tank.createStack(toOutput, Long.MAX_VALUE);
        ChemicalStack remainder = tank.insert(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getAmount() - remainder.getAmount();
        int operations = MachineWorkContext.maxOutputOperations(toOutput.getAmount(), availableRoom);
        if (operations > 0 && !allowFinalProduction(
            toOutput, scaledAmount(toOutput.getAmount(), operations))) {
            operations = 0;
        }
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && tank.getNeeded() > 0L) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
    }

    public static void capFluid(OperationTracker tracker, RecipeError notEnoughSpace, IExtendedFluidTank tank,
                                FluidStack toOutput) {
        if (toOutput.isEmpty()) {
            return;
        }
        FluidStack maxOutput = new FluidStack(toOutput, Integer.MAX_VALUE);
        FluidStack remainder = tank.insert(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getAmount() - remainder.getAmount();
        int operations = MachineWorkContext.maxOutputOperations(toOutput.getAmount(), availableRoom);
        if (operations > 0 && !allowFinalProduction(
            toOutput, scaledAmount(toOutput.getAmount(), operations))) {
            operations = 0;
        }
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && tank.getNeeded() > 0L) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
    }

    private static boolean allowFinalProduction(ItemStack output, long amount) {
        return FinalProductionGate.tryConsume(owner(), output, amount, tick());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean allowFinalProduction(ChemicalStack output, long amount) {
        return FinalProductionGate.tryConsume(owner(), output, amount, tick());
    }

    private static boolean allowFinalProduction(FluidStack output, long amount) {
        return FinalProductionGate.tryConsume(owner(), output, amount, tick());
    }

    private static long scaledAmount(long baseAmount, int operations) {
        if (operations <= 0 || baseAmount <= 0L) {
            return 0L;
        }
        if (MachineWorkContext.scaling()) {
            return MachineWorkContext.scaleOutput(baseAmount, operations);
        }
        long result = baseAmount * (long) operations;
        return result < 0L || result > Long.MAX_VALUE ? Long.MAX_VALUE : result;
    }

    private static UUID owner() {
        MekProcessingContext.Context context = MekProcessingContext.current();
        return context == null ? GearHeartOwnerResolver.SHARED_OWNER : context.owner();
    }

    private static long tick() {
        MekProcessingContext.Context context = MekProcessingContext.current();
        return context == null ? 0L : context.tick();
    }
}
