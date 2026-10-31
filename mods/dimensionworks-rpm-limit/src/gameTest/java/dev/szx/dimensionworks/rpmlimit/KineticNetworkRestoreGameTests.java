package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KineticNetworkRestoreGameTests {

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void restoredMembersResyncWhenTotalsAreUnchanged(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(3, 1, 3);
        BlockPos shaftPos = motorPos.east();
        BlockState motorState = createBlock("creative_motor")
            .setValue(CreativeMotorBlock.FACING, Direction.EAST);
        BlockState shaftState = createBlock("shaft")
            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        helper.setBlock(motorPos, motorState);
        helper.setBlock(shaftPos, shaftState);

        helper.runAfterDelay(20, () -> {
            BlockEntity blockEntity = helper.getBlockEntity(motorPos);
            helper.assertTrue(blockEntity instanceof CreativeMotorBlockEntity,
                "Creative motor did not initialize");
            CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) blockEntity;
            helper.assertTrue(motor.hasNetwork(), "Creative motor did not form a kinetic network");
            KineticNetwork network = motor.getOrCreateNetwork();
            network.updateNetwork();

            float capacity = network.calculateCapacity();
            float stress = network.calculateStress();
            helper.assertTrue(capacity >= stress,
                "Fixture must have enough capacity to clear the stale overload state");

            for (KineticBlockEntity member : network.members.keySet()) {
                member.updateFromNetwork(0, Math.max(1, stress + 1), network.getSize());
                helper.assertTrue(member.isOverStressed(), "Fixture failed to set stale overload state");
            }

            dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.onMemberLoadedSilently(network, motor);
            helper.assertTrue(motor.networkDirty, "Loaded member was not scheduled for a network refresh");

            // The restored totals match Create's current totals, so its normal updateNetwork path
            // skips sync(). The load refresh hook must still sync each member once.
            network.updateNetwork();
            for (KineticBlockEntity member : network.members.keySet()) {
                helper.assertFalse(member.isOverStressed(),
                    "Stale overload flag survived the unchanged-total network refresh");
            }

            for (KineticBlockEntity member : network.members.keySet()) {
                network.members.put(member, 100_000_000f);
            }
            dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.invalidate(network);
            float overloadedStress = network.calculateStress();
            helper.assertTrue(overloadedStress > network.calculateCapacity(),
                "Fixture must be able to exceed its real network capacity");
            network.updateNetwork();
            for (KineticBlockEntity member : network.members.keySet()) {
                helper.assertTrue(member.isOverStressed(),
                    "A genuinely overloaded network must stop after a normal refresh");
                member.updateFromNetwork(network.calculateCapacity(), 0,
                    network.getSize());
            }

            dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.onMemberLoadedSilently(network, motor);
            network.updateNetwork();
            for (KineticBlockEntity member : network.members.keySet()) {
                helper.assertTrue(member.isOverStressed(),
                    "Load synchronization must preserve a genuine overload");
            }
            helper.succeed();
        });
    }

    private static BlockState createBlock(String id) {
        return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
            new net.minecraft.resources.ResourceLocation("create", id)).defaultBlockState();
    }
}
