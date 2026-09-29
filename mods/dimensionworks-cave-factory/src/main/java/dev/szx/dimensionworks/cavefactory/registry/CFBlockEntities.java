package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryPortBlockEntity;
import dev.szx.dimensionworks.cavefactory.blockentity.FluidStabilizerBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class CFBlockEntities {
    public static RegistryObject<BlockEntityType<FactoryControllerBlockEntity>> CONTROLLER;
    public static RegistryObject<BlockEntityType<FactoryPortBlockEntity>> PORT;
    public static RegistryObject<BlockEntityType<FluidStabilizerBlockEntity>> STABILIZER;

    private CFBlockEntities() {}

    public static void register() {
        CONTROLLER = DimensionWorksCaveFactory.CFRegistry.BLOCK_ENTITIES.register(
            "controller",
            () -> {
                net.minecraft.world.level.block.Block[] controllers =
                    new net.minecraft.world.level.block.Block[MachineType.values().length];
                for (MachineType machine : MachineType.values()) {
                    controllers[machine.ordinal()] = CFBlocks.controller(machine).get();
                }
                return BlockEntityType.Builder.of(
                    FactoryControllerBlockEntity::new,
                    controllers
                ).build(null);
            }
        );
        PORT = DimensionWorksCaveFactory.CFRegistry.BLOCK_ENTITIES.register(
            "port",
            () -> BlockEntityType.Builder.of(
                FactoryPortBlockEntity::new,
                CFBlocks.FACTORY_PORT.get()
            ).build(null)
        );
        STABILIZER = DimensionWorksCaveFactory.CFRegistry.BLOCK_ENTITIES.register(
            "fluid_stabilizer",
            () -> BlockEntityType.Builder.of(
                FluidStabilizerBlockEntity::new,
                CFBlocks.FLUID_STABILIZER.get()
            ).build(null)
        );
    }
}
