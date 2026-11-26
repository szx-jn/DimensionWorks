package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = WDTransfer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PackagerItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(BuiltInRegistries.ITEM, WDTransfer.MOD_ID);

    private static final Rarity TRANSCENDENT = Rarity.valueOf("DIMENSIONWORKS_TRANSFER_TRANSCENDENT");

    public static final DeferredHolder<Item, TieredPackager> PACKAGER = ITEMS.register("elastic_packager",
        () -> new TieredPackager(new Item.Properties().rarity(TRANSCENDENT), PackagerProfiles.GREEN));
    public static final DeferredHolder<Item, TieredPackager> BLOCK_PACKAGER = ITEMS.register("block_packager",
        () -> new TieredPackager(new Item.Properties().rarity(TRANSCENDENT), PackagerProfiles.BLUE));
    public static final DeferredHolder<Item, TieredPackager> ADVANCED_PACKAGER = ITEMS.register("advanced_packager",
        () -> new TieredPackager(new Item.Properties().rarity(TRANSCENDENT), PackagerProfiles.PURPLE));

    @SubscribeEvent
    public static void addToOriginalPackagerTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(
                ResourceLocation.fromNamespaceAndPath("create_mobile_packages", "create_mobile_packages_tab"))) return;
        event.accept(PACKAGER.get());
        event.accept(BLOCK_PACKAGER.get());
        event.accept(ADVANCED_PACKAGER.get());
    }

    private PackagerItems() {}
}
