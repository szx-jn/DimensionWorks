package dev.szx.dimensionworks.pressurecrystals;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(DimensionWorksPressureCrystals.MOD_ID)
public final class DimensionWorksPressureCrystals {
    public static final String MOD_ID = "dimensionworks_pressure_crystals";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final RegistryObject<Item> LOW_PRESSURE_CRYSTAL = registerCrystal("low_pressure_crystal");
    public static final RegistryObject<Item> MEDIUM_PRESSURE_CRYSTAL = registerCrystal("medium_pressure_crystal");
    public static final RegistryObject<Item> HIGH_PRESSURE_CRYSTAL = registerCrystal("high_pressure_crystal");

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dimensionworks_pressure_crystals"))
            .icon(() -> new ItemStack(LOW_PRESSURE_CRYSTAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(LOW_PRESSURE_CRYSTAL.get());
                output.accept(MEDIUM_PRESSURE_CRYSTAL.get());
                output.accept(HIGH_PRESSURE_CRYSTAL.get());
            }).build());
    }

    public DimensionWorksPressureCrystals() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static RegistryObject<Item> registerCrystal(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }
}
