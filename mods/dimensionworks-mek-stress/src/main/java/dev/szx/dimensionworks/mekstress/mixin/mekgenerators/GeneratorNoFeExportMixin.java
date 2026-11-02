package dev.szx.dimensionworks.mekstress.mixin.mekgenerators;

import java.util.EnumSet;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.generators.common.tile.TileEntityGenerator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import mekanism.common.util.CableUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep generator production intact while suppressing all FE cable emission. */
@Mixin(value = TileEntityGenerator.class, remap = false)
public abstract class GeneratorNoFeExportMixin {

    @Redirect(method = "onUpdateServer", at = @At(value = "INVOKE",
        target = "Lmekanism/common/util/CableUtils;emit(Ljava/util/EnumSet;Lmekanism/api/energy/IEnergyContainer;Lnet/minecraft/world/level/block/entity/BlockEntity;Lmekanism/api/math/FloatingLong;)V"), remap = false)
    private void dimensionworks$suppressFeExport(EnumSet<Direction> sides, IEnergyContainer container,
                                                  BlockEntity tile, FloatingLong maxOutput) {
        // Mekanism generation remains internal; the adjacent Create shaft exports its SU equivalent.
    }

    @Inject(method = "onUpdateServer", at = @At("TAIL"), remap = false)
    private void dimensionworks$convertStoredFeToMechanicalOutput(CallbackInfo ci) {
        TileEntityGenerator generator = (TileEntityGenerator) (Object) this;
        IEnergyContainer container = generator.getEnergyContainer();
        if (container != null && !container.getEnergy().isZero()) {
            container.extract(container.getEnergy(), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }
}
