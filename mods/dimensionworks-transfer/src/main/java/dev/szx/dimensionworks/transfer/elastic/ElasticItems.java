package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ElasticItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WDTransfer.MOD_ID);
    private static final Rarity GREEN = Rarity.create("DIMENSIONWORKS_ELASTIC", ChatFormatting.GREEN);
    public static final RegistryObject<ElasticPackager> PACKAGER = ITEMS.register("elastic_packager",
        () -> new ElasticPackager(new Item.Properties().rarity(GREEN)));

    private ElasticItems() {}
}
