package dev.szx.dimensionworks.mekstress.mixin.create;

import dev.szx.dimensionworks.mekstress.integration.gas.GasFluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.simibubi.create.content.fluids.FlowSource.FluidHandler.class, remap = false)
public abstract class GasFlowSourceMixin {
    @Redirect(method = "manageSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"), remap = false)
    private LazyOptional<IFluidHandler> dimensionworks$gasEndpoint(BlockEntity be, Capability<IFluidHandler> cap, Direction side) {
        return GasFluidHandler.capability(be, cap, side);
    }
}
