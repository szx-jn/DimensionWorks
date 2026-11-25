package dev.szx.dimensionworks.transfer.elastic;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Compatibility alias for existing elastic behavior and its regression tests. */
public final class ElasticItems {
    public static final DeferredHolder<Item, ElasticPackager> PACKAGER = PackagerItems.PACKAGER;

    private ElasticItems() {}
}
