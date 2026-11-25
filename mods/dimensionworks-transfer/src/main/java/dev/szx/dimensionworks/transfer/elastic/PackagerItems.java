package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PackagerItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(BuiltInRegistries.ITEM, WDTransfer.MOD_ID);

    private static final Rarity GREEN = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_ELASTIC");
    private static final Rarity BLUE = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_BLOCK");
    private static final Rarity PURPLE = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_ADVANCED");

    public static final DeferredHolder<Item, TieredPackager> PACKAGER = ITEMS.register("elastic_packager",
        () -> new TieredPackager(new Item.Properties().rarity(GREEN), PackagerProfiles.GREEN));
    public static final DeferredHolder<Item, TieredPackager> BLOCK_PACKAGER = ITEMS.register("block_packager",
        () -> new TieredPackager(new Item.Properties().rarity(BLUE), PackagerProfiles.BLUE));
    public static final DeferredHolder<Item, TieredPackager> ADVANCED_PACKAGER = ITEMS.register("advanced_packager",
        () -> new TieredPackager(new Item.Properties().rarity(PURPLE), PackagerProfiles.PURPLE));

    private PackagerItems() {}
}
