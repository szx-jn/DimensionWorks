package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.integration.BasinGasHandler;
import java.util.List;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinGasIntegrationMixin {
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

        if (outputItems.size() != 1 || !outputFluids.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack marker = outputItems.get(0);
        CompoundTag tag = marker.getTag();
        if (tag == null || !tag.contains("Gas", Tag.TAG_STRING) || !tag.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
            cir.setReturnValue(false);
            return;
        }

        // Heated Create recipes also match a seething burner. Reject during
        // output simulation as well, so the alternate recipe can be selected
        // without consuming ingredients or producing the wrong gas.
        if (tag.getBoolean("ExcludeSuperheated")
            && ((BasinBlockEntity) (Object) this).getHeatLevel() == HeatLevel.SEETHING) {
            cir.setReturnValue(false);
            return;
        }

        ResourceLocation gasId = ResourceLocation.tryParse(tag.getString("Gas"));
        Gas gas = gasId == null ? null : MekanismAPI.gasRegistry().getValue(gasId);
        long amount = tag.getLong("Amount") * marker.getCount();
        if (gas == null || gas.isEmptyType() || amount <= 0) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(dimensionworks$gasHandler.acceptProducedGas(gas, amount, simulate));
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
