package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DispatchItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, WDTransfer.MOD_ID);
    public static final DeferredHolder<Item, DispatchUpgradeItem> TWILIGHT_UPGRADE = ITEMS.register("twilight_dispatch_upgrade",
        () -> new DispatchUpgradeItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    private DispatchItems() {}
}
