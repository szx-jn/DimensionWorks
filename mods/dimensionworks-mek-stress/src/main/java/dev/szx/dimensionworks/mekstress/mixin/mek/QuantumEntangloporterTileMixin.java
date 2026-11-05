package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import dev.szx.dimensionworks.mekstress.integration.QuantumEntangloporterFrequencyAccess;
import java.util.Map;
import mekanism.api.Coord4D;
import mekanism.common.content.entangloporter.InventoryFrequency;
import mekanism.common.tile.TileEntityQuantumEntangloporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides an incompatible global frequency from the rejected endpoint's own item/fluid/energy capabilities. */
@Mixin(value = TileEntityQuantumEntangloporter.class, remap = false)
public abstract class QuantumEntangloporterTileMixin {
    @Inject(method = "getFreq", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$hideDisallowedFrequency(CallbackInfoReturnable<InventoryFrequency> cir) {
        InventoryFrequency frequency = cir.getReturnValue();
        TileEntityQuantumEntangloporter self = (TileEntityQuantumEntangloporter) (Object) this;
        if (frequency == null || self.getLevel() == null
            || !(frequency instanceof QuantumEntangloporterFrequencyAccess access)) return;
        String dimension = self.getLevel().dimension().location().toString();
        Map<Coord4D, TileEntityQuantumEntangloporter> active = access.dimensionworks$activeEntangloporters();
        for (TileEntityQuantumEntangloporter other : active.values()) {
            if (other == self || other.getLevel() == null) continue;
            if (!DimensionTransportPolicy.legacyTransportAllowed(
                dimension, other.getLevel().dimension().location().toString())) {
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
