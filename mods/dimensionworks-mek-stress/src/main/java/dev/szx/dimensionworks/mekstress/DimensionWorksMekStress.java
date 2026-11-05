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
import dev.szx.dimensionworks.mekstress.memory.MemoryLegacyMigration;
import dev.szx.dimensionworks.mekstress.memory.MachineKineticRegistry;
import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportManager;
import dev.szx.dimensionworks.mekstress.memory.MatterBallMailbox;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DimensionWorksMekStress.MOD_ID)
public final class DimensionWorksMekStress {
    public static final String MOD_ID = "dimensionworks_mek_stress";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<BlockEntityType<CrossDimensionTransportBlockEntity>> CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("cross_dimension_transmitter", DimensionWorksMekStress::createCrossDimensionTransportBlockEntity);
    public static final RegistryObject<BlockEntityType<MatterBallUnpackerBlockEntity>> MATTER_BALL_UNPACKER_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("matter_ball_unpacker", DimensionWorksMekStress::createMatterBallUnpackerBlockEntity);
    public static final RegistryObject<MatterBallUnpackerBlock> MATTER_BALL_UNPACKER = BLOCKS.register(
        "matter_ball_unpacker", () -> new MatterBallUnpackerBlock(
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5F, 6.0F).requiresCorrectToolForDrops(),
            MATTER_BALL_UNPACKER_BLOCK_ENTITY));
    private static final RegistryObject<Item> MATTER_BALL_UNPACKER_ITEM = ITEMS.register(
        "matter_ball_unpacker", () -> new BlockItem(MATTER_BALL_UNPACKER.get(), new Item.Properties()));

    private static final Map<MemoryTier, RegistryObject<MemoryDriveBlock>> DRIVE_BLOCKS = new EnumMap<>(MemoryTier.class);
    private static final Map<Integer, RegistryObject<CrossDimensionTransportBlock>> TRANSPORT_BLOCKS = new java.util.TreeMap<>();
    private static final Map<MemoryTier, Map<MemoryCardType, RegistryObject<MemoryCardItem>>> MEMORY_CARDS =
        new EnumMap<>(MemoryTier.class);

    static {
        BlockBehaviour.Properties driveProperties = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.5F, 6.0F)
            .requiresCorrectToolForDrops();
        for (MemoryTier tier : MemoryTier.values()) {
            String driveName = "memory_drive_ddr" + tier.index();
            RegistryObject<MemoryDriveBlock> block = BLOCKS.register(driveName,
                () -> new MemoryDriveBlock(tier, driveProperties));
            DRIVE_BLOCKS.put(tier, block);
            ITEMS.register(driveName, () -> new BlockItem(block.get(), new Item.Properties()));

            Map<MemoryCardType, RegistryObject<MemoryCardItem>> cards = new EnumMap<>(MemoryCardType.class);
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
            RegistryObject<CrossDimensionTransportBlock> block = BLOCKS.register(name,
                () -> new CrossDimensionTransportBlock(transportTier,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.5F, 8.0F).requiresCorrectToolForDrops(),
                    CROSS_DIMENSION_TRANSPORT_BLOCK_ENTITY));
            TRANSPORT_BLOCKS.put(tier, block);
            ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static final RegistryObject<Item> GAS_OUTPUT_ITEM = ITEMS.register(
        "gas_output", () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> MATTER_BALL = ITEMS.register(
        "matter_ball", () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> ITEM_CAPACITY_UPGRADE = ITEMS.register(
        "item_capacity_upgrade", () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> FLUID_CAPACITY_UPGRADE = ITEMS.register(
        "fluid_capacity_upgrade", () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<MemoryDriveBlockEntity>> MEMORY_DRIVE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("memory_drive", DimensionWorksMekStress::createMemoryDriveBlockEntity);

    private static BlockEntityType<MemoryDriveBlockEntity> createMemoryDriveBlockEntity() {
        Block[] blocks = DRIVE_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
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
        Block[] blocks = TRANSPORT_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
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

    public static final RegistryObject<MenuType<MemoryDriveMenu>> MEMORY_DRIVE_MENU = MENUS.register(
        "memory_drive",
        () -> IForgeMenuType.create(MemoryDriveMenu::new)
    );

    public static final RegistryObject<MenuType<CrossDimensionTransportMenu>> CROSS_DIMENSION_TRANSPORT_MENU = MENUS.register(
        "cross_dimension_transmitter",
        () -> IForgeMenuType.create(CrossDimensionTransportMenu::new)
    );

    public DimensionWorksMekStress() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        dev.szx.dimensionworks.mekstress.integration.gas.GasFluid.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MekStressConfig.SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::buildCreativeTabs);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(MemoryCardCraftingHandler::onItemCrafted);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onMissingMappings);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onEntityLeave);
        MinecraftForge.EVENT_BUS.addListener(MatterBallMailbox::onLogin);
        MinecraftForge.EVENT_BUS.addListener(MatterBallMailbox::clearAtShutdown);
        MinecraftForge.EVENT_BUS.addListener(MachineKineticRegistry::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MachineKineticRegistry::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(CrossDimensionTransportManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(CrossDimensionTransportManager::onServerStopping);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MemoryGridService.register();
            dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork.registerAdapter(
                tile -> tile instanceof mekanism.common.tile.base.TileEntityMekanism,
                tile -> {
                    var route = dev.szx.dimensionworks.mekstress.memory.MachinePowerManager.powerRoute(
                        (mekanism.common.tile.base.TileEntityMekanism) tile);
                    return route == null ? null : route.source();
                });
        });
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            DRIVE_BLOCKS.values().forEach(event::accept);
            TRANSPORT_BLOCKS.values().forEach(event::accept);
            event.accept(MATTER_BALL);
            event.accept(ITEM_CAPACITY_UPGRADE);
            event.accept(FLUID_CAPACITY_UPGRADE);
            event.accept(MATTER_BALL_UNPACKER_ITEM);
            allMemoryCards().forEach(event::accept);
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MemoryCommand.register(event.getDispatcher());
    }

    public static RegistryObject<MemoryCardItem> memoryCard(MemoryTier tier, MemoryCardType type) {
        return MEMORY_CARDS.get(tier).get(type);
    }

    public static List<RegistryObject<MemoryCardItem>> allMemoryCards() {
        return MEMORY_CARDS.values().stream()
            .flatMap(cards -> cards.values().stream())
            .toList();
    }

    public static RegistryObject<MemoryDriveBlock> memoryDrive(MemoryTier tier) {
        return DRIVE_BLOCKS.get(tier);
    }

    private static BlockEntityType<MemoryDriveBlockEntity> blockEntityType() {
        return MEMORY_DRIVE_BLOCK_ENTITY.get();
    }
}
