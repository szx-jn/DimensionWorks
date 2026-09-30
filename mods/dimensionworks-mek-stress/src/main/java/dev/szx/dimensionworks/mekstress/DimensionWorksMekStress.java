package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.block.MemoryDriveBlock;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.card.MemoryCardItem;
import dev.szx.dimensionworks.mekstress.card.MemoryCardCraftingHandler;
import dev.szx.dimensionworks.mekstress.command.MemoryCommand;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import dev.szx.dimensionworks.mekstress.memory.MemoryGridService;
import dev.szx.dimensionworks.mekstress.memory.MemoryLegacyMigration;
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

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    private static final Map<MemoryTier, RegistryObject<MemoryDriveBlock>> DRIVE_BLOCKS = new EnumMap<>(MemoryTier.class);
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
    }

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

    public static final RegistryObject<MenuType<MemoryDriveMenu>> MEMORY_DRIVE_MENU = MENUS.register(
        "memory_drive",
        () -> IForgeMenuType.create(MemoryDriveMenu::new)
    );

    public DimensionWorksMekStress() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
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
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(MemoryLegacyMigration::onEntityLeave);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MemoryGridService.register();
        });
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            DRIVE_BLOCKS.values().forEach(event::accept);
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
