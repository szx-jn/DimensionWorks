package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registers the three DimensionWorks tiers; the white tier is CMP's native item. */
public final class PackagerItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, WDTransfer.MOD_ID);
    private static final Rarity GREEN = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_ELASTIC");
    private static final Rarity BLUE = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_BLOCK");
    private static final Rarity PURPLE = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_ADVANCED");

    public static final DeferredHolder<Item, ElasticPackager> PACKAGER = ITEMS.register("elastic_packager",
        () -> new ElasticPackager(new Item.Properties().rarity(GREEN)));
    public static final DeferredHolder<Item, BluePackager> BLOCK_PACKAGER = ITEMS.register("block_packager",
        () -> new BluePackager(new Item.Properties().rarity(BLUE)));
    public static final DeferredHolder<Item, PurplePackager> ADVANCED_PACKAGER = ITEMS.register("advanced_packager",
        () -> new PurplePackager(new Item.Properties().rarity(PURPLE)));

    private PackagerItems() {}
}
