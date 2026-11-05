package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.me.cluster.implementations.QuantumCluster;
import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the legacy transport policy only to fixed QNB grid connections. */
@Mixin(value = QuantumCluster.class, remap = false)
public abstract class QuantumBridgeDimensionMixin {
    @Redirect(method = "updateStatus", at = @At(value = "INVOKE",
        target = "Lappeng/api/networking/GridHelper;createConnection(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;)Lappeng/api/networking/IGridConnection;"))
    private IGridConnection dimensionworks$restrictBridge(IGridNode first, IGridNode second) {
        if (!(first.getOwner() instanceof QuantumBridgeBlockEntity a)
            || !(second.getOwner() instanceof QuantumBridgeBlockEntity b)
            || a.getLevel() == null || b.getLevel() == null) return null;
        if (!DimensionTransportPolicy.legacyTransportAllowed(
            a.getLevel().dimension().location().toString(), b.getLevel().dimension().location().toString())) return null;
        return GridHelper.createConnection(first, second);
    }
}
