package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import dev.szx.dimensionworks.mekstress.integration.QuantumEntangloporterFrequencyAccess;
import java.util.Map;
import mekanism.common.content.entangloporter.InventoryFrequency;
import mekanism.common.tile.TileEntityQuantumEntangloporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only permits a Mekanism entangloporter frequency to activate across the three vanilla dimensions. */
@Mixin(value = InventoryFrequency.class, remap = false)
public abstract class QuantumEntangloporterFrequencyMixin implements QuantumEntangloporterFrequencyAccess {
    @Shadow @Final private com.google.common.collect.Table<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, net.minecraft.core.BlockPos, TileEntityQuantumEntangloporter> activeQEs;

    @Unique
    @Override
    public java.util.Collection<TileEntityQuantumEntangloporter> dimensionworks$activeEntangloporters() {
        return activeQEs.values();
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$blockDisallowedDimension(BlockEntity blockEntity,
        CallbackInfoReturnable<Boolean> cir) {
        if (!(blockEntity instanceof TileEntityQuantumEntangloporter candidate) || candidate.getLevel() == null) return;
        String dimension = candidate.getLevel().dimension().location().toString();
        for (TileEntityQuantumEntangloporter other : activeQEs.values()) {
            if (other == candidate || other.getLevel() == null) continue;
            String otherDimension = other.getLevel().dimension().location().toString();
            if (!DimensionTransportPolicy.standardTransportAllowed(dimension, otherDimension)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
