package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.card.StressOutputCardItem;
import dev.szx.dimensionworks.mekstress.card.StressOutputCardMenu;
import dev.szx.dimensionworks.mekstress.card.StressOutputCardNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(DimensionWorksMekStress.MOD_ID)
public final class DimensionWorksMekStress {

    public static final String MOD_ID = "dimensionworks_mek_stress";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<StressOutputCardItem> STRESS_OUTPUT_CARD = ITEMS.register(
        "stress_output_card",
        () -> new StressOutputCardItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<MenuType<StressOutputCardMenu>> STRESS_OUTPUT_CARD_MENU = MENUS.register(
        "stress_output_card",
        () -> IForgeMenuType.create(StressOutputCardMenu::new)
    );

    public DimensionWorksMekStress() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        MENUS.register(modBus);
        StressOutputCardNetwork.register();
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MekStressConfig.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(StressOutputCardItem::registerExportBusUpgrade);
    }

    private void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(STRESS_OUTPUT_CARD);
        }
    }
}
