package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ElasticItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, WDTransfer.MOD_ID);
    private static final Rarity GREEN = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_ELASTIC");
    public static final DeferredHolder<Item, ElasticPackager> PACKAGER = ITEMS.register("elastic_packager",
        () -> new ElasticPackager(new Item.Properties().rarity(GREEN)));

    private ElasticItems() {}
}
