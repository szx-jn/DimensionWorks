package dev.szx.dimensionworks.mekstress.mixin.mekgenerators;

import java.util.Set;
import mekanism.api.energy.IEnergyContainer;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorPort;
import mekanism.generators.common.tile.turbine.TileEntityTurbineValve;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {TileEntityTurbineValve.class, TileEntityFusionReactorPort.class}, remap = false)
public abstract class MultiblockNoFeExportMixin {
    @Redirect(method = "onUpdateServer", at = @At(value = "INVOKE",
        target = "Lmekanism/common/util/CableUtils;emit(Ljava/util/Set;Lmekanism/api/energy/IEnergyContainer;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), remap = false)
    private void dimensionworks$noElectricalExport(Set<Direction> sides, IEnergyContainer energy, BlockEntity tile) {
        // Shared production is metered once by MultiblockGenerationMixin.
    }
}
