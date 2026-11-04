package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import dev.szx.dimensionworks.mekstress.integration.gas.GasVent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class GasOpenPipeMixin {
    @Inject(method = "provideHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$ventGas(CallbackInfoReturnable<LazyOptional<IFluidHandler>> cir) {
        OpenEndedPipe pipe = (OpenEndedPipe) (Object) this;
        IFluidHandler delegate = cir.getReturnValue().orElse(null);
        if (delegate == null) return;
        cir.setReturnValue(LazyOptional.of(() -> new IFluidHandler() {
            public int getTanks() { return delegate.getTanks(); }
            public FluidStack getFluidInTank(int tank) { return delegate.getFluidInTank(tank); }
            public int getTankCapacity(int tank) { return delegate.getTankCapacity(tank); }
            public boolean isFluidValid(int tank, FluidStack stack) { return GasFluid.isGas(stack) || delegate.isFluidValid(tank, stack); }
            public int fill(FluidStack stack, FluidAction action) {
                if (!GasFluid.isGas(stack)) return delegate.fill(stack, action);
                Level level = pipe.getWorld(); BlockPos pos = pipe.getOutputPos();
                if (GasFluid.decode(stack).isEmpty() || !GasVent.vent(level, pos, action.simulate())) return 0;
                return stack.getAmount();
            }
            public FluidStack drain(FluidStack stack, FluidAction action) { return delegate.drain(stack, action); }
            public FluidStack drain(int amount, FluidAction action) { return delegate.drain(amount, action); }
        }));
    }
}
