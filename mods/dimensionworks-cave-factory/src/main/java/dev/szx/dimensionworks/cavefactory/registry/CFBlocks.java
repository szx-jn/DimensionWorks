package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.block.FactoryCasingBlock;
import dev.szx.dimensionworks.cavefactory.block.FactoryControllerBlock;
import dev.szx.dimensionworks.cavefactory.block.FactoryPortBlock;
import dev.szx.dimensionworks.cavefactory.block.FluidStabilizerBlock;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CFBlocks {
    private static final Map<MachineType, RegistryObject<FactoryControllerBlock>> CONTROLLERS =
        new EnumMap<>(MachineType.class);
    private static boolean registered;

    public static RegistryObject<FactoryCasingBlock> FACTORY_CASING;
    public static RegistryObject<FactoryPortBlock> FACTORY_PORT;
    public static RegistryObject<FluidStabilizerBlock> FLUID_STABILIZER;
    public static final Map<String, RegistryObject<Block>> CRYSTALLIZED_MOLTEN_METALS = new LinkedHashMap<>();
    private static final List<String> MOLTEN_METAL_IDS = List.of(
        "iron", "copper", "gold", "netherite", "zinc", "brass", "tin", "lead", "osmium",
        "uranium", "bronze", "steel", "refined_obsidian", "refined_glowstone", "scarlet_neodymium",
        "azure_neodymium", "totemic_gold", "poseidite", "shulkerate", "sculkium", "eternium"
    );

    private CFBlocks() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        FACTORY_CASING = DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
            "factory_casing",
            () -> new FactoryCasingBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .strength(4.0F, 8.0F)
                .requiresCorrectToolForDrops())
        );

        FACTORY_PORT = DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
            "factory_port",
            () -> new FactoryPortBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .strength(4.0F, 8.0F)
                .requiresCorrectToolForDrops())
        );

        FLUID_STABILIZER = DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
            "fluid_stabilizer",
            () -> new FluidStabilizerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .strength(5.0F, 10.0F)
                .requiresCorrectToolForDrops())
        );

        for (String metalId : MOLTEN_METAL_IDS) {
            CRYSTALLIZED_MOLTEN_METALS.put(
                "molten_" + metalId,
                DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
                    "crystallized_molten_" + metalId,
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                        .strength(1.5F, 3.0F))
                )
            );
        }

        for (MachineType machine : MachineType.values()) {
            CONTROLLERS.put(
                machine,
                DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
                    machine.id(),
                    () -> new FactoryControllerBlock(
                        machine,
                        BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .strength(5.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                    )
                )
            );
        }
    }

    public static RegistryObject<FactoryControllerBlock> controller(MachineType machine) {
        return CONTROLLERS.get(machine);
    }
}
