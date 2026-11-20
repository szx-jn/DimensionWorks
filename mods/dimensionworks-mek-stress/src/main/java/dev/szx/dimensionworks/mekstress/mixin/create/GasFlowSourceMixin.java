package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.foundation.ICapabilityProvider;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluidHandler;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=com.simibubi.create.content.fluids.FlowSource.FluidHandler.class, remap=false)
public abstract class GasFlowSourceMixin {
    @Unique private Level dimensionworks$world;
    @Unique private BlockPos dimensionworks$endpoint;
    @Unique private Direction dimensionworks$side;
    @Unique private WeakReference<BlockEntity> dimensionworks$controller;
    @Unique private ICapabilityProvider<IFluidHandler> dimensionworks$provider;

    @Inject(method="manageSource", at=@At("TAIL"), remap=false)
    private void dimensionworks$manageChemicals(Level level, BlockEntity controller, CallbackInfo ci) {
        var face = ((FlowSourceLocationAccessor)(Object)this).dimensionworks$location();
        if (dimensionworks$provider != null && dimensionworks$world == level
            && dimensionworks$endpoint.equals(face.getConnectedPos()) && dimensionworks$controller.get() == controller) return;
        dimensionworks$world = level; dimensionworks$endpoint = face.getConnectedPos();
        dimensionworks$side = face.getOppositeFace(); dimensionworks$controller = new WeakReference<>(controller);
        dimensionworks$provider = () -> {
            var owner = dimensionworks$controller.get();
            return owner == null || owner.isRemoved() ? null : GasFluidHandler.capability(
                dimensionworks$world, dimensionworks$endpoint, dimensionworks$side);
        };
    }
    @Inject(method="provideHandler", at=@At("RETURN"), cancellable=true, remap=false)
    private void dimensionworks$provideChemicals(CallbackInfoReturnable<ICapabilityProvider<IFluidHandler>> cir) {
        if (dimensionworks$provider != null) cir.setReturnValue(dimensionworks$provider);
    }
}
