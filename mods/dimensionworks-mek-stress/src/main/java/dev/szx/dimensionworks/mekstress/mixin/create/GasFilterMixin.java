package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import mekanism.api.Action;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let a filled gas container act as a Create fluid filter for that gas. */
@Mixin(value = FilterItemStack.class, remap = false)
public abstract class GasFilterMixin {
    @Shadow @Final private ItemStack filterItemStack;
    @Shadow private FluidStack filterFluidStack;
    @Shadow private void resolveFluid(Level level) {}

    @Inject(method = "resolveFluid", at = @At("RETURN"), remap = false)
    private void dimensionworks$resolveGas(Level level, CallbackInfo ci) {
        if (!filterFluidStack.isEmpty()) return;
        IGasHandler handler = filterItemStack.getCapability(Capabilities.GAS_HANDLER).orElse(null);
        if (handler != null) filterFluidStack = GasFluid.encode(handler.extractChemical(1, Action.SIMULATE));
    }

    @Inject(method = "test(Lnet/minecraft/world/level/Level;Lnet/minecraftforge/fluids/FluidStack;Z)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$matchGasIdentity(Level level, FluidStack stack, boolean matchNbt, CallbackInfoReturnable<Boolean> cir) {
        if (!GasFluid.isGas(stack) || filterItemStack.isEmpty()) return;
        resolveFluid(level);
        if (GasFluid.isGas(filterFluidStack)) {
            cir.setReturnValue(filterFluidStack.isFluidEqual(stack));
        }
    }
}
