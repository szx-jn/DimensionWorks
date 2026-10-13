package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartOwnerResolver;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import dev.szx.dimensionworks.rpmlimit.quota.MekProcessingContext;
import dev.szx.dimensionworks.mekstress.integration.mek.MachineOutputDelivery;
import dev.szx.dimensionworks.mekstress.integration.mek.ScaledOutputHandler;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OutputHelper.class, remap = false)
public abstract class OutputHelperMixin {
    @Inject(
        method = "handleOutput(Lmekanism/api/inventory/IInventorySlot;Lnet/minecraft/world/item/ItemStack;I)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void dimensionworks$scaleItemOutput(IInventorySlot slot, ItemStack toOutput, int operations,
                                                        CallbackInfo ci) {
        if (operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        MekProcessingContext.Context context = MekProcessingContext.current();
        UUID owner = context == null ? GearHeartOwnerResolver.SHARED_OWNER : context.owner();
        long tick = context == null ? 0L : context.tick();
        long gateAmount = MachineWorkContext.scaling()
            ? MachineWorkContext.scaleOutput(toOutput.getCount(), operations)
            : (long) toOutput.getCount() * operations;
        if (!hasItemSpace(slot, toOutput, gateAmount)
            || !FinalProductionGate.tryConsume(owner, toOutput, gateAmount, tick)) {
            ci.cancel();
            return;
        }
        if (!MachineWorkContext.scaling()) {
            return;
        }
        long count = MachineWorkContext.scaleOutput(toOutput.getCount(), operations);
        if (count <= 0L) {
            ci.cancel();
            return;
        }
        dimensionworks$insertItems(slot, toOutput, count);
        ci.cancel();
    }

    @Inject(
        method = "handleOutput(Lmekanism/api/chemical/IChemicalTank;Lmekanism/api/chemical/ChemicalStack;I)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$scaleChemicalOutput(IChemicalTank tank, ChemicalStack toOutput, int operations,
                                                            CallbackInfo ci) {
        if (operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        MekProcessingContext.Context context = MekProcessingContext.current();
        UUID owner = context == null ? GearHeartOwnerResolver.SHARED_OWNER : context.owner();
        long tick = context == null ? 0L : context.tick();
        long gateAmount = MachineWorkContext.scaling()
            ? MachineWorkContext.scaleOutput(toOutput.getAmount(), operations)
            : saturatingMultiply(toOutput.getAmount(), operations);
        if (!hasChemicalSpace(tank, toOutput, gateAmount)
            || !FinalProductionGate.tryConsume(owner, toOutput, gateAmount, tick)) {
            ci.cancel();
            return;
        }
        if (!MachineWorkContext.scaling()) {
            return;
        }
        long amount = MachineWorkContext.scaleOutput(toOutput.getAmount(), operations);
        if (amount <= 0L) {
            ci.cancel();
            return;
        }
        tank.insert(tank.createStack(toOutput, amount), Action.EXECUTE, AutomationType.INTERNAL);
        ci.cancel();
    }

    @Inject(
        method = "handleOutput(Lmekanism/api/fluid/IExtendedFluidTank;Lnet/minecraftforge/fluids/FluidStack;I)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void dimensionworks$scaleFluidOutput(IExtendedFluidTank tank, FluidStack toOutput, int operations,
                                                         CallbackInfo ci) {
        if (operations <= 0 || toOutput.isEmpty()) {
            return;
        }
        MekProcessingContext.Context context = MekProcessingContext.current();
        UUID owner = context == null ? GearHeartOwnerResolver.SHARED_OWNER : context.owner();
        long tick = context == null ? 0L : context.tick();
        long gateAmount = MachineWorkContext.scaling()
            ? MachineWorkContext.scaleOutput(toOutput.getAmount(), operations)
            : saturatingMultiply(toOutput.getAmount(), operations);
        if (!hasFluidSpace(tank, toOutput, gateAmount)
            || !FinalProductionGate.tryConsume(owner, toOutput, gateAmount, tick)) {
            ci.cancel();
            return;
        }
        if (!MachineWorkContext.scaling()) {
            return;
        }
        long amount = MachineWorkContext.scaleOutput(toOutput.getAmount(), operations);
        if (amount <= 0L) {
            ci.cancel();
            return;
        }
        tank.insert(new FluidStack(toOutput, (int) Math.min(Integer.MAX_VALUE, amount)),
            Action.EXECUTE, AutomationType.INTERNAL);
        ci.cancel();
    }

    @Inject(
        method = "calculateOperationsCanSupport(Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/inventory/IInventorySlot;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void dimensionworks$scaleItemOutputCapacity(OperationTracker tracker, RecipeError notEnoughSpace,
                                                                IInventorySlot slot, ItemStack toOutput,
                                                                CallbackInfo ci) {
        if (!MachineWorkContext.scaling() || toOutput.isEmpty()) {
            return;
        }
        ItemStack maxOutput = toOutput.copyWithCount(toOutput.getMaxStackSize());
        ItemStack remainder = slot.insertItem(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getCount() - remainder.getCount();
        int operations = MachineWorkContext.maxOutputOperations(toOutput.getCount(), availableRoom);
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && slot.getLimit(slot.getStack()) - slot.getCount() > 0) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
        ci.cancel();
    }

    @Inject(
        method = "calculateOperationsCanSupport(Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/chemical/IChemicalTank;Lmekanism/api/chemical/ChemicalStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$scaleChemicalOutputCapacity(OperationTracker tracker, RecipeError notEnoughSpace,
                                                                    IChemicalTank tank, ChemicalStack toOutput,
                                                                    CallbackInfo ci) {
        if (!MachineWorkContext.scaling() || toOutput.isEmpty()) {
            return;
        }
        ChemicalStack maxOutput = tank.createStack(toOutput, Long.MAX_VALUE);
        ChemicalStack remainder = tank.insert(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getAmount() - remainder.getAmount();
        int operations = MachineWorkContext.maxOutputOperations(toOutput.getAmount(), availableRoom);
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && tank.getNeeded() > 0L) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
        ci.cancel();
    }

    @Inject(
        method = "calculateOperationsCanSupport(Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/fluid/IExtendedFluidTank;Lnet/minecraftforge/fluids/FluidStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void dimensionworks$scaleFluidOutputCapacity(OperationTracker tracker, RecipeError notEnoughSpace,
                                                                IExtendedFluidTank tank, FluidStack toOutput,
                                                                CallbackInfo ci) {
        if (!MachineWorkContext.scaling() || toOutput.isEmpty()) {
            return;
        }
        FluidStack maxOutput = new FluidStack(toOutput, Integer.MAX_VALUE);
        FluidStack remainder = tank.insert(maxOutput, Action.SIMULATE, AutomationType.INTERNAL);
        long availableRoom = maxOutput.getAmount() - remainder.getAmount();
        int operations = MachineWorkContext.maxOutputOperations(toOutput.getAmount(), availableRoom);
        tracker.updateOperations(operations);
        if (operations == 0) {
            if (availableRoom == 0L && tank.getNeeded() > 0L) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
            } else {
                tracker.addError(notEnoughSpace);
            }
        }
        ci.cancel();
    }

    private static long saturatingMultiply(long value, int operations) {
        if (value <= 0L || operations <= 0) {
            return 0L;
        }
        try {
            return Math.multiplyExact(value, (long) operations);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean hasItemSpace(IInventorySlot slot, ItemStack toOutput, long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (amount > Integer.MAX_VALUE) {
            return false;
        }
        ItemStack remainder = slot.insertItem(
            toOutput.copyWithCount((int) amount), Action.SIMULATE, AutomationType.INTERNAL);
        return remainder.isEmpty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasChemicalSpace(IChemicalTank tank, ChemicalStack toOutput, long amount) {
        if (amount <= 0L) {
            return true;
        }
        ChemicalStack request = tank.createStack(toOutput, amount);
        ChemicalStack remainder = tank.insert(request, Action.SIMULATE, AutomationType.INTERNAL);
        return remainder.isEmpty();
    }

    private static boolean hasFluidSpace(IExtendedFluidTank tank, FluidStack toOutput, long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (amount > Integer.MAX_VALUE) {
            return false;
        }
        FluidStack request = new FluidStack(toOutput, (int) amount);
        FluidStack remainder = tank.insert(request, Action.SIMULATE, AutomationType.INTERNAL);
        return remainder.isEmpty();
    }

    private static void dimensionworks$insertItems(IInventorySlot slot, ItemStack toOutput, long count) {
        if (count <= 0L) {
            return;
        }
        int batchSize = Math.max(1, toOutput.getMaxStackSize());
        long remaining = count;
        while (remaining > 0L) {
            int batchCount = (int) Math.min((long) batchSize, remaining);
            ItemStack remainder = slot.insertItem(toOutput.copyWithCount(batchCount),
                Action.EXECUTE, AutomationType.INTERNAL);
            long inserted = batchCount - (long) remainder.getCount();
            if (inserted <= 0L) {
                return;
            }
            remaining -= inserted;
        }
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/chemical/IChemicalTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapChemicalOutputHandler(IChemicalTank<?, ?> tank, RecipeError error,
                                                                  CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapChemical((IOutputHandler) cir.getReturnValue(),
            (IChemicalTank) tank, error));
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/fluid/IExtendedFluidTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapFluidOutputHandler(IExtendedFluidTank tank, RecipeError error,
                                                              CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapFluid(tank, error, (IOutputHandler) cir.getReturnValue()));
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapItemOutputHandler(IInventorySlot slot, RecipeError error,
                                                             CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapItem(slot, error, (IOutputHandler) cir.getReturnValue()));
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapChanceOutputHandler(IInventorySlot mainSlot, RecipeError mainError,
                                                               IInventorySlot secondarySlot, RecipeError secondaryError,
                                                               CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapChance(mainSlot, mainError, secondarySlot, secondaryError,
            (IOutputHandler) cir.getReturnValue()));
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/chemical/gas/IGasTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapReactionOutputHandler(IInventorySlot slot, RecipeError slotError,
                                                                 IGasTank tank, RecipeError tankError,
                                                                 CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapReaction(slot, slotError, tank, tankError,
            (IOutputHandler) cir.getReturnValue()));
    }

    @Inject(
        method = "getOutputHandler(Lmekanism/api/chemical/gas/IGasTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;Lmekanism/api/chemical/gas/IGasTank;Lmekanism/api/recipes/cache/CachedRecipe$OperationTracker$RecipeError;)Lmekanism/api/recipes/outputs/IOutputHandler;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dimensionworks$wrapElectrolysisOutputHandler(IGasTank leftTank, RecipeError leftError,
                                                                     IGasTank rightTank, RecipeError rightError,
                                                                     CallbackInfoReturnable<IOutputHandler<?>> cir) {
        cir.setReturnValue(ScaledOutputHandler.wrapElectrolysis(leftTank, leftError, rightTank, rightError,
            (IOutputHandler) cir.getReturnValue()));
    }
}
