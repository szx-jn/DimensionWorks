package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** KubeJS adapter for data-preserving packager tier upgrades. */
public final class ElasticPackager {
    private ElasticPackager() {}

    public static ItemStack upgrade(ItemStack original) {
        return upgrade(original, PackagerItems.PACKAGER.get());
    }

    public static ItemStack upgradeToBlue(ItemStack original) {
        return upgrade(original, PackagerItems.BLOCK_PACKAGER.get());
    }

    public static ItemStack upgradeToPurple(ItemStack original) {
        return upgrade(original, PackagerItems.ADVANCED_PACKAGER.get());
    }

    private static ItemStack upgrade(ItemStack original, Item target) {
        return original.transmuteCopy(target, 1);
    }
}
