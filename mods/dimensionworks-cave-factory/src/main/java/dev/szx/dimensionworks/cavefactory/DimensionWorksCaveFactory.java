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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Mod(DimensionWorksCaveFactory.MOD_ID)
public final class DimensionWorksCaveFactory {
    public static final String MOD_ID = "dimensionworks_cave_factory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public DimensionWorksCaveFactory(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CaveFactoryConfig.SPEC);
        CFRegistry.register(modBus);
        CFCapabilities.ATTACHMENTS.register(modBus);
        dev.szx.dimensionworks.cavefactory.registry.CFDataComponents.REGISTER.register(modBus);
        CFEvents.register();
        modBus.addListener(CFEvents::registerCapabilities);
        LOGGER.info("DimensionWorks Cave Factory loaded");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }

    public static final class CFRegistry {
        public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK, MOD_ID);
        public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, MOD_ID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, MOD_ID);
        public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MOD_ID);
        public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_TYPE, MOD_ID);
        public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, MOD_ID);
        public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
        public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.FLUID, MOD_ID);
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
