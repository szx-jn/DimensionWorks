package dev.szx.dimensionworks.cavefactory;

import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import dev.szx.dimensionworks.cavefactory.registry.CFMenus;
import dev.szx.dimensionworks.cavefactory.registry.CFRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Mod(DimensionWorksCaveFactory.MOD_ID)
public final class DimensionWorksCaveFactory {
    public static final String MOD_ID = "dimensionworks_cave_factory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public DimensionWorksCaveFactory(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CaveFactoryConfig.SPEC);
        IEventBus modBus = context.getModEventBus();
        CFRegistry.register(modBus);
        CFForgeEvents.register();
        modBus.addListener(CFForgeEvents::registerCapabilities);
        modBus.addListener(CFForgeEvents::commonSetup);
        LOGGER.info("DimensionWorks Cave Factory loaded");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path.toLowerCase(Locale.ROOT));
    }

    public static final class CFRegistry {
        public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
        public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
        public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
        public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MOD_ID);
        public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
        public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
        public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);
        public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);

        private CFRegistry() {}

        public static void register(IEventBus bus) {
            CFFluids.register();
            CFBlocks.register();
            CFBlockEntities.register();
            CFItems.register();
            CFMenus.register();
            CFRecipes.register();

            BLOCKS.register(bus);
            ITEMS.register(bus);
            BLOCK_ENTITIES.register(bus);
            MENUS.register(bus);
            RECIPE_TYPES.register(bus);
            RECIPE_SERIALIZERS.register(bus);
            FLUID_TYPES.register(bus);
            FLUIDS.register(bus);
            CREATIVE_TABS.register(bus);
        }
    }
}
