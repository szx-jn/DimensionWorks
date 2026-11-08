package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.integration.BasinGasHandler;
import java.util.List;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinGasIntegrationMixin {
    @Invoker(value = "getHeatLevel", remap = false)
    public abstract HeatLevel dimensionworks$getHeatLevel();

    @Invoker(value = "acceptOutputs", remap = false)
    public abstract boolean dimensionworks$acceptRegularOutputs(
        List<ItemStack> items, List<FluidStack> fluids, boolean simulate);

    @Unique
    private BasinGasHandler dimensionworks$gasHandler =
        new BasinGasHandler((BasinBlockEntity) (Object) this);

    @Inject(method = "acceptOutputs", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$acceptGasRecipeOutput(
        List<ItemStack> outputItems,
        List<FluidStack> outputFluids,
        boolean simulate,
        CallbackInfoReturnable<Boolean> cir
    ) {
        boolean hasMarker = outputItems.stream()
            .anyMatch(stack -> stack.is(DimensionWorksMekStress.GAS_OUTPUT_ITEM.get()));
        if (!hasMarker) {
            return;
        }

        List<ItemStack> markers = outputItems.stream()
            .filter(stack -> stack.is(DimensionWorksMekStress.GAS_OUTPUT_ITEM.get())).toList();
        if (markers.size() != 1) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack marker = markers.get(0);
        CompoundTag tag = marker.getTag();
        if (tag == null) {
            cir.setReturnValue(false);
            return;
        }

        // Heated Create recipes also match a seething burner. Reject during
        // output simulation as well, so the alternate recipe can be selected
        // without consuming ingredients or producing the wrong gas.
        if (tag.getBoolean("ExcludeSuperheated")
            && dimensionworks$getHeatLevel() == HeatLevel.SEETHING) {
            cir.setReturnValue(false);
            return;
        }

        boolean inputOnly = tag.getBoolean("InputOnly");
        Gas gas = null;
        long amount = 0L;
        if (!inputOnly) {
            if (!tag.contains("Gas", Tag.TAG_STRING) || !tag.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
                cir.setReturnValue(false);
                return;
            }
            ResourceLocation gasId = ResourceLocation.tryParse(tag.getString("Gas"));
            gas = gasId == null ? null : MekanismAPI.gasRegistry().getValue(gasId);
            amount = dimensionworks$scaledAmount(tag, "Amount", marker.getCount());
            if (gas == null || gas.isEmptyType() || amount <= 0) {
                cir.setReturnValue(false);
                return;
            }
        }

        Gas inputGas = null;
        long inputAmount = 0L;
        if (inputOnly || tag.contains("InputGas") || tag.contains("InputAmount")) {
            ResourceLocation inputId = ResourceLocation.tryParse(tag.getString("InputGas"));
            inputGas = inputId == null ? null : MekanismAPI.gasRegistry().getValue(inputId);
            inputAmount = dimensionworks$scaledAmount(tag, "InputAmount", marker.getCount());
            if (inputGas == null || inputGas.isEmptyType() || inputAmount <= 0) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Validate gas before invoking Create's normal output handling. The
        // recursive call has no marker, so it takes the original item/fluid path.
        // Matching only simulates; execution commits once, on the server thread.
        if (!dimensionworks$gasHandler.processRecipeGas(inputGas, inputAmount, gas, amount, true)) {
            cir.setReturnValue(false);
            return;
        }
        List<ItemStack> regularItems = outputItems.stream()
            .filter(stack -> !stack.is(DimensionWorksMekStress.GAS_OUTPUT_ITEM.get())).toList();
        if (!dimensionworks$acceptRegularOutputs(regularItems, outputFluids, simulate)) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(
            dimensionworks$gasHandler.processRecipeGas(inputGas, inputAmount, gas, amount, simulate));
    }

    @Unique
    private static long dimensionworks$scaledAmount(CompoundTag tag, String key, int count) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC) || tag.getLong(key) <= 0 || count <= 0) return -1L;
        try {
            return Math.multiplyExact(tag.getLong(key), (long) count);
        } catch (ArithmeticException overflow) {
            return -1L;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$leakExposedGas(CallbackInfo ci) {
        dimensionworks$gasHandler.leakIfDue();
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void dimensionworks$readGas(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        dimensionworks$gasHandler.readFromNbt(tag);
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void dimensionworks$writeGas(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        dimensionworks$gasHandler.writeToNbt(tag);
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$exposeGasCapability(
        Capability<?> capability,
        Direction side,
        CallbackInfoReturnable<LazyOptional<?>> cir
    ) {
        if (capability == Capabilities.GAS_HANDLER) {
            cir.setReturnValue(dimensionworks$gasHandler.capability().cast());
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void dimensionworks$invalidateGasCapability(CallbackInfo ci) {
        dimensionworks$gasHandler.invalidate();
    }
}
