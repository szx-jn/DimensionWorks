package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import dev.szx.dimensionworks.cavefactory.item.ModuleItem;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@net.neoforged.fml.common.EventBusSubscriber(modid = DimensionWorksCaveFactory.MOD_ID,
    bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class CFItems {
    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void keepDedicatedItemsInSharedTab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(CREATIVE_TAB.getId())
            || event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.SEARCH) return;
        java.util.function.Predicate<ItemStack> dedicated = stack -> {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null && (id.getNamespace().equals("dimensionworks") || id.getNamespace().startsWith("dimensionworks_") || id.getNamespace().equals("kubejs"));
        };
        java.util.stream.Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream())
            .filter(dedicated).toList()
            .forEach(stack -> event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }


    private static final Map<MachineType, DeferredHolder<Item, Item>> MATRICES = new EnumMap<>(MachineType.class);
    private static final Map<MachineType, DeferredHolder<Item, Item>> RESIDUES = new EnumMap<>(MachineType.class);
    private static final Map<MachineType, DeferredHolder<Item, BlockItem>> CONTROLLER_ITEMS =
        new EnumMap<>(MachineType.class);
    private static final Map<FactoryModule, DeferredHolder<Item, Item>> MODULES =
        new EnumMap<>(FactoryModule.class);
    private static final List<DeferredHolder<Item, ? extends Item>> CREATIVE_ORDER = new ArrayList<>();

    public static DeferredHolder<Item, BlockItem> FACTORY_CASING;
    public static DeferredHolder<Item, BlockItem> FACTORY_PORT;
    public static DeferredHolder<Item, BlockItem> FLUID_STABILIZER;
    public static DeferredHolder<Item, FluidAmpouleItem> FLUID_AMPOULE;
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB;

    private static boolean registered;

    private CFItems() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        FACTORY_CASING = registerBlockItem("factory_casing", CFBlocks.FACTORY_CASING);
        FACTORY_PORT = registerBlockItem("factory_port", CFBlocks.FACTORY_PORT);
        FLUID_STABILIZER = registerBlockItem("fluid_stabilizer", CFBlocks.FLUID_STABILIZER);
        for (MachineType machine : MachineType.values()) {
            CONTROLLER_ITEMS.put(
                machine,
                registerBlockItem(machine.id(), CFBlocks.controller(machine))
            );
            MATRICES.put(
                machine,
                registerSimpleItem(machine.matrixId(), new Item.Properties().stacksTo(64))
            );
            RESIDUES.put(
                machine,
                registerSimpleItem(machine.residueId(), new Item.Properties().stacksTo(64))
            );
        }

        FLUID_AMPOULE = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
            "fluid_ampoule",
            () -> new FluidAmpouleItem(new Item.Properties().stacksTo(16))
        );
        CREATIVE_ORDER.add(FLUID_AMPOULE);

        for (FactoryModule module : FactoryModule.values()) {
            DeferredHolder<Item, Item> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
                module.id(),
                () -> new ModuleItem(module, new Item.Properties().stacksTo(1))
            );
            MODULES.put(module, holder);
            CREATIVE_ORDER.add(holder);
        }

        CREATIVE_TAB = DimensionWorksCaveFactory.CFRegistry.CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.dimensionworks_cave_factory"))
                .icon(() -> new ItemStack(CFBlocks.controller(MachineType.ABYSSAL).get()))
                .displayItems((parameters, output) -> {
                    // One shared tab for all pack-owned namespaces, including future additions.
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
                        .filter(entry -> entry.getKey().location().getNamespace().equals("dimensionworks")
                            || entry.getKey().location().getNamespace().startsWith("dimensionworks_")
                            || entry.getKey().location().getNamespace().equals("kubejs"))
                        .filter(entry -> !entry.getKey().location().toString().equals("dimensionworks_mek_stress:gas_output"))
                        .sorted(java.util.Comparator.comparing(entry -> entry.getKey().location().toString()))
                        .forEach(entry -> output.accept(entry.getValue()));
                })
                .build()
        );
    }

    public static DeferredHolder<Item, Item> matrix(MachineType machine) {
        return MATRICES.get(machine);
    }

    public static DeferredHolder<Item, Item> residue(MachineType machine) {
        return RESIDUES.get(machine);
    }

    public static DeferredHolder<Item, Item> module(FactoryModule module) {
        return MODULES.get(module);
    }

    public static DeferredHolder<Item, BlockItem> controllerItem(MachineType machine) {
        return CONTROLLER_ITEMS.get(machine);
    }

    private static DeferredHolder<Item, BlockItem> registerBlockItem(
        String id,
        DeferredHolder<net.minecraft.world.level.block.Block, ? extends net.minecraft.world.level.block.Block> block
    ) {
        DeferredHolder<Item, BlockItem> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
            id,
            () -> new BlockItem(block.get(), new Item.Properties())
        );
        CREATIVE_ORDER.add(holder);
        return holder;
    }

    private static DeferredHolder<Item, Item> registerSimpleItem(String id, Item.Properties properties) {
        DeferredHolder<Item, Item> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
            id,
            () -> new Item(properties)
        );
        CREATIVE_ORDER.add(holder);
        return holder;
    }
}
