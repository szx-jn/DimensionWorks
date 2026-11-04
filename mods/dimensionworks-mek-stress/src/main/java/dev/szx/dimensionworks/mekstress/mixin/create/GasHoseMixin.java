package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import dev.szx.dimensionworks.mekstress.integration.gas.GasVent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidFillingBehaviour.class, remap = false)
public abstract class GasHoseMixin {
    @Inject(method = "tryDeposit", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$ventGas(Fluid fluid, BlockPos root, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (fluid != GasFluid.SOURCE.get()) return;
        BlockEntityBehaviour behaviour = (BlockEntityBehaviour) (Object) this;
        cir.setReturnValue(GasVent.vent(behaviour.getWorld(), root, simulate));
    }
}
