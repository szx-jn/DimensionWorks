package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Compatibility name retained for existing scripts and regression tests. */
public final class ElasticItems {
    public static final DeferredHolder<Item, TieredPackager> PACKAGER = PackagerItems.PACKAGER;

    private ElasticItems() {}
}
