package dev.szx.dimensionworks.mekstress.integration.jade;

import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Resolves Mekanism bounding blocks to their main machine before deciding Jade visibility. */
public final class JadeEnergyVisibility {
    private JadeEnergyVisibility() {
    }

    public static boolean shouldHide(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        if (blockEntity instanceof TileEntityMekanism) {
            return true;
        }
        return blockEntity instanceof TileEntityBoundingBlock bounding
            && bounding.getMainTile(bounding.getMainPos()) instanceof TileEntityMekanism;
    }
}
