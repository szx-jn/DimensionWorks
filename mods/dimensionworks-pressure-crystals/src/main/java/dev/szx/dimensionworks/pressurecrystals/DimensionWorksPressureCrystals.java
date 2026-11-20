package dev.szx.dimensionworks.pressurecrystals;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(DimensionWorksPressureCrystals.MOD_ID)
public final class DimensionWorksPressureCrystals {
    public static final String MOD_ID = "dimensionworks_pressure_crystals";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);
    public static final DeferredHolder<Item, Item> USELESS_CRYSTAL = registerCrystal("useless_crystal");
    public static final DeferredHolder<Item, Item> LOW_PRESSURE_CRYSTAL = registerCrystal("low_pressure_crystal");
    public static final DeferredHolder<Item, Item> MEDIUM_PRESSURE_CRYSTAL = registerCrystal("medium_pressure_crystal");
    public static final DeferredHolder<Item, Item> HIGH_PRESSURE_CRYSTAL = registerCrystal("high_pressure_crystal");

    public DimensionWorksPressureCrystals(IEventBus bus) {
        ITEMS.register(bus);
    }

    private static DeferredHolder<Item, Item> registerCrystal(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }
}
