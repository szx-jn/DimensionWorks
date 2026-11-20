package dev.szx.dimensionworks.rpmlimit.controller;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

public final class AdvancedController {
    public static final String ID = "advanced_speed_controller";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionWorksRpmLimit.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DimensionWorksRpmLimit.MOD_ID);
    public static final DeferredHolder<Block, AdvancedControllerBlock> BLOCK = BLOCKS.register(ID,
        () -> new AdvancedControllerBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
            .mapColor(net.minecraft.world.level.material.MapColor.TERRACOTTA_YELLOW)));
    public static final DeferredHolder<Item, Item> ITEM = ITEMS.register(ID, () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpeedControllerBlockEntity>> ENTITY = ENTITIES.register(ID,
        () -> BlockEntityType.Builder.<SpeedControllerBlockEntity>of(AdvancedControllerEntity::new, BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedControllerMenu>> MENU = MENUS.register(ID,
        () -> IMenuTypeExtension.create((id, inventory, data) -> new AdvancedControllerMenu(id, inventory,
            data.readBlockPos(), data.readInt(), data.readInt(), data.readInt())));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(ControllerNetwork::register);

    }
    private AdvancedController() {}
}
