package dev.szx.dimensionworks.mekstress.mixin.create;

import dev.szx.dimensionworks.mekstress.integration.gas.GasFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.simibubi.create.content.fluids.pump.PumpBlockEntity.class, remap = false)
public abstract class GasPumpMixin {
    @Redirect(method = "hasReachedValidEndpoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object dimensionworks$gasEndpoint(Level level, BlockCapability<?, ?> cap, BlockPos pos, Object side) {
        return GasFluidHandler.capability(level, pos, (Direction) side);
    }
}
