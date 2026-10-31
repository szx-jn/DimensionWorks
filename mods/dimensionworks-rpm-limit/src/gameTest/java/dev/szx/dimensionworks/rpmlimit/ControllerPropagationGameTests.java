package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.controller.AdvancedController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ControllerPropagationGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedOverLimitFromCog(GameTestHelper helper) { check(helper, true, true, 1024, 512, 512); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedReverseOverLimitFromCog(GameTestHelper helper) { check(helper, true, true, -1024, 512, -512); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedOverLimitFromShaft(GameTestHelper helper) { check(helper, true, false, 1024, 512, 512); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedBelowLimit(GameTestHelper helper) { check(helper, true, true, 128, 512, 128); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedZeroTarget(GameTestHelper helper) { check(helper, true, true, 0, 512, 0); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedPackCeiling(GameTestHelper helper) { check(helper, true, true, 10240, 10240, 10240); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedReversePackCeiling(GameTestHelper helper) { check(helper, true, true, -10240, 10240, -10240); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void advancedStageCapAtPackCeiling(GameTestHelper helper) { check(helper, true, true, 10240, 512, 512); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void vanillaOverLimit(GameTestHelper helper) { check(helper, false, true, 256, 32, 32); }

    private static BlockState createBlock(String id) {
        return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
            new net.minecraft.resources.ResourceLocation("create", id)).defaultBlockState();
    }

    private static void check(GameTestHelper helper, boolean advanced, boolean motorOnCog,
                              int target, int limit, int expected) {
        BlockPos controllerPos = new BlockPos(3, 1, 3);
        BlockPos cogPos = controllerPos.above();
        BlockState controllerState = (advanced ? AdvancedController.BLOCK.get().defaultBlockState()
            : createBlock("rotation_speed_controller"))
            .setValue(SpeedControllerBlock.HORIZONTAL_AXIS, Direction.Axis.Z);
        helper.setBlock(controllerPos, controllerState);
        helper.setBlock(cogPos, createBlock("large_cogwheel").setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        SpeedControllerBlockEntity controller = (SpeedControllerBlockEntity) helper.getBlockEntity(controllerPos);
        KineticBlockEntity cog = (KineticBlockEntity) helper.getBlockEntity(cogPos);
        controller.getPersistentData().putUUID("dimensionworks_owner", new java.util.UUID(103, controller.getBlockPos().asLong()));
        cog.getPersistentData().putUUID("dimensionworks_owner", new java.util.UUID(103, cog.getBlockPos().asLong()));
        controller.getPersistentData().putInt("dimensionworks_machine_rpm_limit", limit);
        cog.getPersistentData().putInt("dimensionworks_machine_rpm_limit", limit);
        controller.targetSpeed.setValue(target);
        BlockPos motorPos = motorOnCog ? cogPos.west() : controllerPos.south();
        Direction facing = motorOnCog ? Direction.EAST : Direction.NORTH;
        helper.setBlock(motorPos, createBlock("creative_motor").setValue(CreativeMotorBlock.FACING, facing));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getBlockEntity(motorPos);
        motor.getPersistentData().putUUID("dimensionworks_owner", new java.util.UUID(103, motor.getBlockPos().asLong()));
        motor.getPersistentData().putInt("dimensionworks_machine_rpm_limit", limit);
        helper.runAfterDelay(20, () -> {
            KineticBlockEntity output = motorOnCog ? controller : cog;
            float actual = output.getTheoreticalSpeed();
            helper.assertTrue(Math.abs(actual - expected) < .001f,
                "Expected " + expected + " RPM, got " + actual + " (target " + target + ")");
            helper.assertTrue(!controller.isRemoved() && !cog.isRemoved(), "Transmission was destroyed");
            helper.succeed();
        });
    }
}
