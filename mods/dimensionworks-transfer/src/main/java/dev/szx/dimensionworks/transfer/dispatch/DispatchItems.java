package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DispatchItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WDTransfer.MOD_ID);
    public static final RegistryObject<DispatchUpgradeItem> TWILIGHT_UPGRADE = ITEMS.register("twilight_dispatch_upgrade",
        () -> new DispatchUpgradeItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    private DispatchItems() {}
}
