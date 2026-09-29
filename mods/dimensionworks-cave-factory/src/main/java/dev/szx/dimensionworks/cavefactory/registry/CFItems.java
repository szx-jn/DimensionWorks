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
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CFItems {
    private static final Map<MachineType, RegistryObject<Item>> MATRICES = new EnumMap<>(MachineType.class);
    private static final Map<MachineType, RegistryObject<Item>> RESIDUES = new EnumMap<>(MachineType.class);
    private static final Map<MachineType, RegistryObject<BlockItem>> CONTROLLER_ITEMS =
        new EnumMap<>(MachineType.class);
    private static final Map<FactoryModule, RegistryObject<Item>> MODULES =
        new EnumMap<>(FactoryModule.class);
    private static final List<RegistryObject<? extends Item>> CREATIVE_ORDER = new ArrayList<>();

    public static RegistryObject<BlockItem> FACTORY_CASING;
    public static RegistryObject<BlockItem> FACTORY_PORT;
    public static RegistryObject<BlockItem> FLUID_STABILIZER;
    public static RegistryObject<FluidAmpouleItem> FLUID_AMPOULE;
    public static RegistryObject<CreativeModeTab> CREATIVE_TAB;

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
            RegistryObject<Item> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
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
                    for (RegistryObject<? extends Item> holder : CREATIVE_ORDER) {
                        output.accept(holder.get());
                    }
                })
                .build()
        );
    }

    public static RegistryObject<Item> matrix(MachineType machine) {
        return MATRICES.get(machine);
    }

    public static RegistryObject<Item> residue(MachineType machine) {
        return RESIDUES.get(machine);
    }

    public static RegistryObject<Item> module(FactoryModule module) {
        return MODULES.get(module);
    }

    public static RegistryObject<BlockItem> controllerItem(MachineType machine) {
        return CONTROLLER_ITEMS.get(machine);
    }

    private static RegistryObject<BlockItem> registerBlockItem(
        String id,
        RegistryObject<? extends net.minecraft.world.level.block.Block> block
    ) {
        RegistryObject<BlockItem> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
            id,
            () -> new BlockItem(block.get(), new Item.Properties())
        );
        CREATIVE_ORDER.add(holder);
        return holder;
    }

    private static RegistryObject<Item> registerSimpleItem(String id, Item.Properties properties) {
        RegistryObject<Item> holder = DimensionWorksCaveFactory.CFRegistry.ITEMS.register(
            id,
            () -> new Item(properties)
        );
        CREATIVE_ORDER.add(holder);
        return holder;
    }
}
