package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.block.MemoryDriveBlock;
import dev.szx.dimensionworks.mekstress.block.CrossDimensionTransportBlock;
import dev.szx.dimensionworks.mekstress.block.MatterBallUnpackerBlock;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity;
import dev.szx.dimensionworks.mekstress.blockentity.MatterBallUnpackerBlockEntity;
import dev.szx.dimensionworks.mekstress.card.MemoryCardItem;
import dev.szx.dimensionworks.mekstress.card.MemoryCardCraftingHandler;
import dev.szx.dimensionworks.mekstress.command.MemoryCommand;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import dev.szx.dimensionworks.mekstress.memory.MemoryGridService;
import dev.szx.dimensionworks.mekstress.memory.MachineKineticRegistry;
import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportManager;
import dev.szx.dimensionworks.mekstress.memory.MatterBallMailbox;
import dev.szx.dimensionworks.rpmlimit.api.RpmLimitServices;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DimensionWorksMekStress.MOD_ID)
public final class DimensionWorksMekStress {
    public static final String MOD_ID = "dimensionworks_mek_stress";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossDimensionTransportBlockEntity>> CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("cross_dimension_transmitter", DimensionWorksMekStress::createCrossDimensionTransportBlockEntity);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterBallUnpackerBlockEntity>> MATTER_BALL_UNPACKER_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("matter_ball_unpacker", DimensionWorksMekStress::createMatterBallUnpackerBlockEntity);
    public static final DeferredHolder<Block, MatterBallUnpackerBlock> MATTER_BALL_UNPACKER = BLOCKS.register(
        "matter_ball_unpacker", () -> new MatterBallUnpackerBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F, 6.0F).requiresCorrectToolForDrops(),
            MATTER_BALL_UNPACKER_BLOCK_ENTITY));
    private static final DeferredHolder<Item, Item> MATTER_BALL_UNPACKER_ITEM = ITEMS.register(
        "matter_ball_unpacker", () -> new BlockItem(MATTER_BALL_UNPACKER.get(), new Item.Properties()));

    private static final Map<MemoryTier, DeferredHolder<Block, MemoryDriveBlock>> DRIVE_BLOCKS = new EnumMap<>(MemoryTier.class);
    private static final Map<Integer, DeferredHolder<Block, CrossDimensionTransportBlock>> TRANSPORT_BLOCKS = new java.util.TreeMap<>();
    private static final Map<MemoryTier, Map<MemoryCardType, DeferredHolder<Item, MemoryCardItem>>> MEMORY_CARDS =
        new EnumMap<>(MemoryTier.class);

    static {
        BlockBehaviour.Properties driveProperties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .strength(3.5F, 6.0F)
            .requiresCorrectToolForDrops();
        for (MemoryTier tier : MemoryTier.values()) {
            String driveName = "memory_drive_ddr" + tier.index();
            DeferredHolder<Block, MemoryDriveBlock> block = BLOCKS.register(driveName,
                () -> new MemoryDriveBlock(tier, driveProperties));
            DRIVE_BLOCKS.put(tier, block);
            ITEMS.register(driveName, () -> new BlockItem(block.get(), new Item.Properties()));

            Map<MemoryCardType, DeferredHolder<Item, MemoryCardItem>> cards = new EnumMap<>(MemoryCardType.class);
            for (MemoryCardType cardType : MemoryCardType.values()) {
                String cardName = "memory_card_ddr" + tier.index() + "_" + cardType.id();
                cards.put(cardType, ITEMS.register(cardName,
                    () -> new MemoryCardItem(tier, cardType, new Item.Properties())));
            }
            MEMORY_CARDS.put(tier, Map.copyOf(cards));
        }
        for (int tier = 1; tier <= 3; tier++) {
            int transportTier = tier;
            String name = "cross_dimension_transmitter_tier" + tier;
            DeferredHolder<Block, CrossDimensionTransportBlock> block = BLOCKS.register(name,
                () -> new CrossDimensionTransportBlock(transportTier,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.5F, 8.0F).requiresCorrectToolForDrops(),
                    CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY));
            TRANSPORT_BLOCKS.put(tier, block);
            ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static final DeferredHolder<Item, Item> GAS_OUTPUT_ITEM = ITEMS.register(
        "gas_output", () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> MATTER_BALL = ITEMS.register(
        "matter_ball", () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> ITEM_CAPACITY_UPGRADE = ITEMS.register(
        "item_capacity_upgrade", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> FLUID_CAPACITY_UPGRADE = ITEMS.register(
        "fluid_capacity_upgrade", () -> new Item(new Item.Properties())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MemoryDriveBlockEntity>> MEMORY_DRIVE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("memory_drive", DimensionWorksMekStress::createMemoryDriveBlockEntity);

    private static BlockEntityType<MemoryDriveBlockEntity> createMemoryDriveBlockEntity() {
        Block[] blocks = DRIVE_BLOCKS.values().stream().map(DeferredHolder::get).toArray(Block[]::new);
        BlockEntityType<MemoryDriveBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MemoryDriveBlockEntity(blockEntityType(), pos, state),
            blocks
        ).build(null);
        for (Block block : blocks) {
            if (block instanceof MemoryDriveBlock drive) {
                drive.setBlockEntity(MemoryDriveBlockEntity.class, type, null, null);
            }
        }
        return type;
    }

    private static BlockEntityType<CrossDimensionTransportBlockEntity> createCrossDimensionTransportBlockEntity() {
        Block[] blocks = TRANSPORT_BLOCKS.values().stream().map(DeferredHolder::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(
            (pos, state) -> new CrossDimensionTransportBlockEntity(CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY.get(), pos, state),
            blocks
        ).build(null);
    }

    private static BlockEntityType<MatterBallUnpackerBlockEntity> createMatterBallUnpackerBlockEntity() {
        return BlockEntityType.Builder.of(
            (pos, state) -> new MatterBallUnpackerBlockEntity(MATTER_BALL_UNPACKER_BLOCK_ENTITY.get(), pos, state),
            MATTER_BALL_UNPACKER.get()
        ).build(null);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<MemoryDriveMenu>> MEMORY_DRIVE_MENU = MENUS.register(
        "memory_drive",
        () -> IMenuTypeExtension.create(MemoryDriveMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<CrossDimensionTransportMenu>> CROSS_DIMENSION_TRANSPORT_MENU = MENUS.register(
        "cross_dimension_transmitter",
        () -> IMenuTypeExtension.create(CrossDimensionTransportMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu>> CLOUD_DISK_MENU = MENUS.register(
        "cloud_disk", () -> IMenuTypeExtension.create(dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu::new));

    public DimensionWorksMekStress(IEventBus modBus, ModContainer container) {
        dev.szx.dimensionworks.mekstress.integration.gas.GasFluid.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, MekStressConfig.SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(DimensionWorksMekStress::registerCapabilities);
        modBus.addListener(TransportNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(MemoryCardCraftingHandler::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(MatterBallMailbox::onLogin);
        NeoForge.EVENT_BUS.addListener(MachineKineticRegistry::onServerTick);
        NeoForge.EVENT_BUS.addListener(MachineKineticRegistry::onServerStopping);
        NeoForge.EVENT_BUS.addListener(CrossDimensionTransportManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(CrossDimensionTransportManager::onServerStopping);
    }

    private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
            MEMORY_DRIVE_BLOCK_ENTITY.get(), (be, context) -> be);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
            MEMORY_DRIVE_BLOCK_ENTITY.get(), (be, side) -> be.getExposedItemHandler(side));
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),
            com.simibubi.create.AllBlockEntityTypes.BASIN.get(),
            (be, side) -> ((dev.szx.dimensionworks.mekstress.integration.BasinGasAccess) be).dimensionworks$chemicalHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
            CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY.get(), (be, side) -> be.itemHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
            CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY.get(), (be, side) -> be.fluidHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
            MATTER_BALL_UNPACKER_BLOCK_ENTITY.get(), (be, side) -> be.itemHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
            MATTER_BALL_UNPACKER_BLOCK_ENTITY.get(), (be, side) -> be.fluidHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MemoryGridService.register();
            RpmLimitServices.topology().registerAdapter(
                tile -> tile instanceof mekanism.common.tile.base.TileEntityMekanism,
                tile -> {
                    var route = dev.szx.dimensionworks.mekstress.memory.MachinePowerManager.powerRoute(
                        (mekanism.common.tile.base.TileEntityMekanism) tile);
                    return route == null ? null : route.source();
                });
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MemoryCommand.register(event.getDispatcher());
    }

    public static DeferredHolder<Item, MemoryCardItem> memoryCard(MemoryTier tier, MemoryCardType type) {
        return MEMORY_CARDS.get(tier).get(type);
    }

    public static List<DeferredHolder<Item, MemoryCardItem>> allMemoryCards() {
        return MEMORY_CARDS.values().stream()
            .flatMap(cards -> cards.values().stream())
            .toList();
    }

    public static DeferredHolder<Block, MemoryDriveBlock> memoryDrive(MemoryTier tier) {
        return DRIVE_BLOCKS.get(tier);
    }

    private static BlockEntityType<MemoryDriveBlockEntity> blockEntityType() {
        return MEMORY_DRIVE_BLOCK_ENTITY.get();
    }
}
