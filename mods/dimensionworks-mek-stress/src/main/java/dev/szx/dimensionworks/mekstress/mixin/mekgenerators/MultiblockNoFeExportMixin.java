package dev.szx.dimensionworks.mekstress.mixin.mekgenerators;

import java.util.Set;
import mekanism.api.energy.IEnergyContainer;
import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {TurbineMultiblockData.class, FusionReactorMultiblockData.class}, remap = false)
public abstract class MultiblockNoFeExportMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lmekanism/common/util/CableUtils;emit(Ljava/util/Collection;Lmekanism/api/energy/IEnergyContainer;)V"), remap = false)
    private void dimensionworks$noElectricalExport(java.util.Collection<mekanism.common.integration.energy.BlockEnergyCapabilityCache> outputs,
        IEnergyContainer energy) {
        // Shared production is metered once by MultiblockGenerationMixin.
    }
}
