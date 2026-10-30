package dev.szx.dimensionworks.mekstress;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.szx.dimensionworks.mekstress.api.IMEGearboxExportState;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public class LiveStressGameTests {
    private static BlockState block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void liveAllocationAndImmediateOverload(GameTestHelper helper) {
        BlockPos drivePos = new BlockPos(3, 2, 3);
        BlockPos inputPos = drivePos.west();
        BlockPos outputPos = drivePos.east();
        helper.setBlock(drivePos, block("dimensionworks_mek_stress:memory_drive_ddr1"));
        helper.setBlock(drivePos.south(), block("ae2:creative_energy_cell"));
        helper.setBlock(inputPos, block("appliedcreate:me_gearbox"));
        helper.setBlock(outputPos, block("appliedcreate:me_gearbox"));
        helper.setBlock(drivePos.above(), block("appliedcreate:me_gearbox"));
        MEGearboxBlockEntity input = (MEGearboxBlockEntity) helper.getBlockEntity(inputPos);
        MEGearboxBlockEntity output = (MEGearboxBlockEntity) helper.getBlockEntity(outputPos);
        MEGearboxBlockEntity secondOutput = (MEGearboxBlockEntity) helper.getBlockEntity(drivePos.above());
        secondOutput.setMode(MEGearboxBlockEntity.Mode.EXPORT);
        secondOutput.setConfiguredSpeed(0);
        secondOutput.setConfiguredStress(0);
        output.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 10240);
        secondOutput.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 10240);
        input.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 10240);
        input.setMode(MEGearboxBlockEntity.Mode.IMPORT);
        input.setConfiguredStress(8);
        output.setMode(MEGearboxBlockEntity.Mode.EXPORT);
        output.setConfiguredSpeed(16);
        output.setConfiguredStress(4);
        Direction.Axis axis = ((IRotate) input.getBlockState().getBlock()).getRotationAxis(input.getBlockState());
        Direction towardsMotor = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        BlockPos motorPos = inputPos.relative(towardsMotor);
        helper.setBlock(motorPos, block("create:creative_motor")
            .setValue(CreativeMotorBlock.FACING, towardsMotor.getOpposite()));
        MemoryDriveBlockEntity drive = (MemoryDriveBlockEntity) helper.getBlockEntity(drivePos);
        helper.runAfterDelay(60, () -> {
            // Applied Create coerces zero RPM to one; zero stress explicitly disables this port.
            secondOutput.setConfiguredStress(0);
            helper.assertTrue(drive.getMainNode().isActive(), "Drive must be online");
            IMemoryGridService service = drive.getMainNode().getGrid().getService(IMemoryGridService.class);
            var first = service.snapshot(0);
            helper.assertTrue(first.capacitySu() == 128 && first.allocatedSu() == 64,
                "Expected input 128 and allocation 64, got " + first);
            helper.assertTrue(output.getGeneratedSpeed() == 16, "Output must run at 16 RPM");
            for (int i = 0; i < 100; i++)
                helper.assertTrue(service.snapshot(i).availableSu() == 64, "Reading/time must not drain capacity");
            output.setConfiguredStress(8);
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.OK, "Exact allocation must run");
            output.setConfiguredStress(9);
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.OUTPUT_OVERLOAD,
                "Output over capacity must overload");
            helper.assertTrue(output.getGeneratedSpeed() == 0, "Overload must stop rotation immediately");
            output.setConfiguredStress(4);
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.OK, "Reducing demand must recover");
            input.setConfiguredStress(1000);
            output.setConfiguredStress(1);
            output.setConfiguredSpeed(128);
            secondOutput.setConfiguredSpeed(129);
            secondOutput.setConfiguredStress(1);
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.BANDWIDTH_OVERLOAD,
                "Two ports individually below 256 but totaling 257 RPM must overload: " + service.snapshot(0)
                    + " port2active=" + secondOutput.getMainNode().isActive());
            helper.assertTrue(output.getGeneratedSpeed() == 0 && secondOutput.getGeneratedSpeed() == 0,
                "RPM overload must stop every output");
            drive.getInternalInventory().setItemDirect(0, new ItemStack(ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("dimensionworks_mek_stress:memory_card_ddr1_balanced"))));
            helper.assertTrue(service.snapshot(0).capacitySu() == 16000, "Cards must never add stress capacity");
            helper.assertTrue(service.snapshot(0).effectiveCapacitySu() == 15760,
                "Card must deduct 1.5 percent of live input stress");
            helper.assertTrue(service.snapshot(0).bandwidthRpm() == 512, "Card must add RPM budget");
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.OK,
                "Adding a card must recover aggregate RPM overload");
            drive.getInternalInventory().setItemDirect(0, ItemStack.EMPTY);
            helper.assertTrue(service.snapshot(0).status() == MemoryNetworkStatus.BANDWIDTH_OVERLOAD,
                "Removing card must immediately revoke its RPM budget");
            secondOutput.setConfiguredStress(0);
            output.setConfiguredSpeed(16);
            output.setConfiguredStress(4);
            input.setConfiguredStress(0);
            helper.assertTrue(service.snapshot(0).capacitySu() == 0, "Editing source must immediately change total");
            helper.assertTrue(output.getGeneratedSpeed() == 0, "Zero input must stop outputs");
            input.setConfiguredStress(8);
            helper.assertTrue(service.snapshot(0).capacitySu() == 128, "Source edit must recover without filling energy");
            secondOutput.setMode(MEGearboxBlockEntity.Mode.IMPORT);
            secondOutput.setConfiguredStress(2);
            Direction secondMotorDirection = towardsMotor;
            if (drivePos.above().relative(secondMotorDirection).equals(drivePos))
                secondMotorDirection = secondMotorDirection.getOpposite();
            helper.setBlock(drivePos.above().relative(secondMotorDirection), block("create:creative_motor")
                .setValue(CreativeMotorBlock.FACING, secondMotorDirection.getOpposite()));
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(service.snapshot(0).capacitySu() == 160,
                    "Two independent inputs must sum to 128 + 32: " + service.snapshot(0));
                secondOutput.setConfiguredStress(4);
                helper.assertTrue(service.snapshot(0).capacitySu() == 192, "Editing second source must replace its share");
                helper.setBlock(inputPos, net.minecraft.world.level.block.Blocks.AIR);
                helper.assertTrue(service.snapshot(0).capacitySu() == 64, "Removing first input must retain only second share");
                helper.setBlock(drivePos.above(), net.minecraft.world.level.block.Blocks.AIR);
                helper.assertTrue(service.snapshot(0).capacitySu() == 0, "Removed inputs must not leave phantom capacity");
                helper.assertTrue(output.getGeneratedSpeed() == 0, "Removed sources must stop output");
                helper.setBlock(outputPos, net.minecraft.world.level.block.Blocks.AIR);
                helper.assertTrue(service.snapshot(0).allocatedSu() == 0, "Removed output must release its reservation");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void legacyStoredStressCannotPowerGrid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, block("dimensionworks_mek_stress:memory_drive_ddr1"));
        helper.setBlock(pos.south(), block("ae2:creative_energy_cell"));
        helper.setBlock(pos.east(), block("appliedcreate:me_gearbox"));
        MemoryDriveBlockEntity drive = (MemoryDriveBlockEntity) helper.getBlockEntity(pos);
        var legacy = drive.saveWithoutMetadata();
        legacy.putLong("StoredSu", Long.MAX_VALUE);
        drive.loadTag(legacy);
        MEGearboxBlockEntity output = (MEGearboxBlockEntity) helper.getBlockEntity(pos.east());
        output.setMode(MEGearboxBlockEntity.Mode.EXPORT);
        output.setConfiguredSpeed(16);
        output.setConfiguredStress(4);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(drive.getMainNode().isActive(), "Legacy drive must be online");
            var snapshot = drive.getMainNode().getGrid().getService(IMemoryGridService.class).snapshot(0);
            helper.assertTrue(snapshot.capacitySu() == 0 && snapshot.effectiveCapacitySu() == 0,
                "Legacy StoredSu must not restore capacity");
            helper.assertTrue(snapshot.status() == MemoryNetworkStatus.OUTPUT_OVERLOAD,
                "Zero capacity must report output overload");
            helper.assertTrue(output.getGeneratedSpeed() == 0, "Legacy stock must never power output");
            helper.assertTrue(!drive.saveWithoutMetadata().contains("StoredSu"), "Save must retire legacy stock");
            helper.succeed();
        });
    }
}
