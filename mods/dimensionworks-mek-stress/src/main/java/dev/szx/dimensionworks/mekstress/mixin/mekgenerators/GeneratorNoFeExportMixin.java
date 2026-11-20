package dev.szx.dimensionworks.mekstress.mixin.mekgenerators;

import java.util.Set;
import mekanism.api.energy.IEnergyContainer;
import mekanism.generators.common.tile.TileEntityGenerator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep generator production intact while suppressing all FE cable emission. */
@Mixin(value = TileEntityGenerator.class, remap = false)
public abstract class GeneratorNoFeExportMixin {

    @Redirect(method = "onUpdateServer", at = @At(value = "INVOKE",
        target = "Lmekanism/common/util/CableUtils;emit(Ljava/util/Collection;Lmekanism/api/energy/IEnergyContainer;J)V"), remap = false)
    private void dimensionworks$suppressFeExport(java.util.Collection<mekanism.common.integration.energy.BlockEnergyCapabilityCache> outputs,
        IEnergyContainer container, long maxOutput) {
        // Mekanism generation remains internal; the adjacent Create shaft exports its SU equivalent.
    }

}
