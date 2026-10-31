package dev.szx.dimensionworks.mekstress.mixin.draconic;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorStabilizer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve the component tick and redstone updates, but stop passive OP/FE export. */
@Mixin(value = TileReactorStabilizer.class, remap = false)
public abstract class ReactorStabilizerMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/brandon3055/brandonscore/utils/EnergyUtils;canReceiveEnergy(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/core/Direction;)Z"
        ),
        remap = false
    )
    private boolean dimensionworks$noPassiveFeExport(BlockEntity neighbor, Direction side) {
        return false;
    }
}
