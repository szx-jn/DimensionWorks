package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/** Blue tier: green capabilities plus ordinary Mekanism cardboard-box block packing. */
public class BluePackager extends ElasticPackager {
    public BluePackager(Item.Properties properties) { super(properties); }

    @Override public boolean canPackBlocks() { return true; }

    public static ItemStack upgrade(ItemStack original) {
        return original.transmuteCopy(PackagerItems.BLOCK_PACKAGER.get(), 1);
    }
}
