package dev.szx.dimensionworks.cavefactory;

import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.FactoryDimension;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.logic.StructureLayout;
import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in fixture runs only in an isolated new server world and stops normally after its checks. */
@EventBusSubscriber(modid = "dimensionworks_cave_factory")
public final class CaveServerCheck {
    private CaveServerCheck() {}
    @SubscribeEvent public static void check(ServerStartedEvent event) {
        if (!Boolean.getBoolean("dimensionworks.verifyCaveServer")) return;
        var server = event.getServer();
        try {
            for (var dimension : FactoryDimension.values()) {
                var key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension.dimensionId()));
                require(server.getLevel(key) != null, "Missing cave dimension " + key.location());
            }
            ServerLevel abyssal = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(FactoryDimension.ABYSSAL.dimensionId())));
            BlockPos center = new BlockPos(8, 200, 8);
            abyssal.getChunk(center);
            if (Boolean.getBoolean("dimensionworks.verifyCaveReload")) {
                var restored = (FactoryControllerBlockEntity) abyssal.getBlockEntity(center);
                require(restored != null && count(restored, "pearl") == 1 && count(restored, "heavy_bone") == 1
                    && count(restored, "sea_glass_shards") == 2 && restored.outputTank().getFluidAmount() == 500,
                    "Factory output lost after full server restart");
                require(restored.phase() == FactoryPhase.PHASE_B && restored.phaseProgress() == 1 && restored.owner() != null,
                    "Factory phase or owner lost after full server restart");
                DimensionWorksCaveFactory.LOGGER.info("CAVE RELOAD CHECK PASSED: persisted cave machine outputs, fluids, phase and owner after new process startup");
                return;
            }

            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                var block = StructureLayout.isCenter(x, y, z) ? CFBlocks.controller(MachineType.ABYSSAL).get()
                    : StructureLayout.isFaceCenter(x, y, z) ? CFBlocks.FACTORY_PORT.get() : CFBlocks.FACTORY_CASING.get();
                var state = block.defaultBlockState();
                if (StructureLayout.isFaceCenter(x, y, z)) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
                    x != 0 ? net.minecraft.core.Direction.Axis.X : y != 0 ? net.minecraft.core.Direction.Axis.Y : net.minecraft.core.Direction.Axis.Z);
                abyssal.setBlock(center.offset(x, y, z), state, 3);
            }
            var machine = (FactoryControllerBlockEntity) abyssal.getBlockEntity(center);
            require(machine.validateStructure(), "Actual abyssal factory did not assemble");
            var owner = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(abyssal);
            machine.setOwner(owner);
            require(machine.cyclePort(net.minecraft.core.Direction.EAST, owner), "Kinetic port did not configure");
            var motorPos = center.offset(2, 0, 0);
            abyssal.setBlock(motorPos, com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.WEST), 3);
            var motor = (com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity) abyssal.getBlockEntity(motorPos);
            motor.initialize();
            runPhase(machine, FactoryPhase.PHASE_A);
            require(count(machine, "pearl") == 1 && machine.outputTank().getFluidAmount() == 250, "Authored primary phase output changed");
            runPhase(machine, FactoryPhase.PHASE_B);
            require(count(machine, "heavy_bone") == 1 && count(machine, "sea_glass_shards") == 2
                && machine.outputTank().getFluidAmount() == 500, "Authored secondary phase output changed");
            require(machine.outputTank().getFluid().is(CFFluids.get(MachineType.ABYSSAL).still().get()), "Factory brine identity changed");
            var saved = machine.saveWithFullMetadata(abyssal.registryAccess());
            var reloaded = (FactoryControllerBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(center,
                machine.getBlockState(), saved, abyssal.registryAccess());
            require(reloaded != null && count(reloaded, "pearl") == 1 && count(reloaded, "heavy_bone") == 1
                && count(reloaded, "sea_glass_shards") == 2 && reloaded.outputTank().getFluidAmount() == 500, "Actual cave outputs lost on reload");
            DimensionWorksCaveFactory.LOGGER.info("CAVE SERVER CHECK PASSED: six cave dimensions, generated abyssal chunk, both authored phases and native save reload");
        } finally { server.halt(false); }
    }
    private static void runPhase(FactoryControllerBlockEntity machine, FactoryPhase phase) {
        machine.setPhaseState(phase, 0);
        machine.inputItems().setStackInSlot(0, stack("abyssmarine", 4));
        machine.inputItems().setStackInSlot(1, stack("bioluminesscence", 2));
        machine.inputItems().setStackInSlot(2, stack("marine_snow", 2));
        // Ticks the real powered port and controller while startup holds the ordinary world loop.
        var port = (dev.szx.dimensionworks.cavefactory.blockentity.FactoryPortBlockEntity) machine.getLevel().getBlockEntity(machine.getBlockPos().east());
        for (int tick = 0; tick < 200 && !machine.inputItems().getStackInSlot(0).isEmpty(); tick++) { port.tick(); machine.tick(); }
        System.out.println("CAVE PHASE STATE: " + phase + ", rpm=" + machine.effectiveRpm() + ", structure=" + machine.structureValid()
            + ", progress=" + machine.saveWithoutMetadata(machine.getLevel().registryAccess()).getInt("ProcessProgress"));
        for (int slot = 0; slot < 3; slot++) require(machine.inputItems().getStackInSlot(slot).isEmpty(), "Authored phase did not consume original inputs");
    }
    private static ItemStack stack(String id, int count) {
        var key = ResourceLocation.fromNamespaceAndPath("alexscaves", id);
        require(BuiltInRegistries.ITEM.containsKey(key), "Missing Alex item " + key);
        return new ItemStack(BuiltInRegistries.ITEM.get(key), count);
    }
    private static int count(FactoryControllerBlockEntity machine, String id) {
        int count = 0; var item = stack(id, 1).getItem();
        for (int slot = 0; slot < machine.outputItems().getSlots(); slot++) {
            var content = machine.outputItems().getStackInSlot(slot); if (content.is(item)) count += content.getCount();
        }
        return count;
    }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
