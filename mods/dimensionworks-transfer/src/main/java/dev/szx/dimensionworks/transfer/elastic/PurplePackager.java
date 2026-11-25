package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/** Purple tier: blue capabilities plus extended compatibility and special ship capture. */
public final class PurplePackager extends BluePackager {
    public PurplePackager(Item.Properties properties) { super(properties); }

    @Override public boolean canPackSableShips() { return true; }
    @Override public boolean hasExtendedBlockCompatibility() { return true; }

    public static ItemStack upgrade(ItemStack original) {
        return original.transmuteCopy(PackagerItems.ADVANCED_PACKAGER.get(), 1);
    }
}
