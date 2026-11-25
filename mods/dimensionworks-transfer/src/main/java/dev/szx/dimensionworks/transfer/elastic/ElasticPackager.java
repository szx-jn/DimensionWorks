package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.ItemStack;

/** KubeJS upgrade adapter for the green-tier tool. */
public final class ElasticPackager {
    private ElasticPackager() {}

    public static ItemStack upgrade(ItemStack original) {
        return original.transmuteCopy(PackagerItems.PACKAGER.get(), 1);
    }
}
