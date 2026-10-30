package dev.szx.dimensionworks.rpmlimit.controller;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class AdvancedController {
    public static final String ID = "advanced_speed_controller";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DimensionWorksRpmLimit.MOD_ID);
    public static final RegistryObject<AdvancedControllerBlock> BLOCK = BLOCKS.register(ID,
        () -> new AdvancedControllerBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
            .mapColor(net.minecraft.world.level.material.MapColor.TERRACOTTA_YELLOW)));
    public static final RegistryObject<Item> ITEM = ITEMS.register(ID, () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<SpeedControllerBlockEntity>> ENTITY = ENTITIES.register(ID,
        () -> BlockEntityType.Builder.<SpeedControllerBlockEntity>of(AdvancedControllerEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<AdvancedControllerMenu>> MENU = MENUS.register(ID,
        () -> IForgeMenuType.create((id, inventory, data) -> new AdvancedControllerMenu(id, inventory,
            data.readBlockPos(), data.readInt(), data.readInt(), data.readInt())));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        ControllerNetwork.register();
        bus.addListener((net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(ITEM.get());
        });
    }
    private AdvancedController() {}
}
