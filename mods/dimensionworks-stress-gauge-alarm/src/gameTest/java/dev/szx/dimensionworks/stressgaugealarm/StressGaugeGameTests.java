package dev.szx.dimensionworks.stressgaugealarm;

import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder(DimensionWorksStressGaugeAlarm.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StressGaugeGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void comparatorAndOverloadSignals(GameTestHelper helper) {
        AllConfigs.server().kinetics.disableStress.set(false);
        BlockPos pos = new BlockPos(2, 1, 2);
        BlockPos absolutePos = helper.absolutePos(pos);
        BlockState state = BuiltInRegistries.BLOCK
            .get(ResourceLocation.fromNamespaceAndPath("create", "stressometer")).defaultBlockState();
        helper.setBlock(pos, state);
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.REDSTONE_WIRE);
        helper.assertTrue(state.isSignalSource(), "应力表必须声明红石源，红石粉才能连接");
        StressGaugeBlockEntity gauge = (StressGaugeBlockEntity) helper.getBlockEntity(pos);

        gauge.updateFromNetwork(100, 50, 1);
        helper.assertTrue(Math.abs(gauge.dialTarget - 0.5F) < 0.001F,
            "Create stressometer should use 50% network utilization, got " + gauge.dialTarget);
        int analog = ((GaugeBlock) state.getBlock()).getAnalogOutputSignal(state, helper.getLevel(), absolutePos);
        helper.assertTrue(analog == 7, "50% stress should produce analog strength 7, got " + analog);

        gauge.updateFromNetwork(100, 125, 1);
        int alarm = state.getSignal(helper.getLevel(), absolutePos, Direction.NORTH);
        helper.assertTrue(gauge.isOverStressed(), "125 SU on 100 SU capacity should be overstressed");
        helper.assertTrue(alarm == 15, "overstressed gauge should emit full redstone, got " + alarm);
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).getValue(RedStoneWireBlock.POWER) == 15,
            "过载信号必须实际传到邻接红石粉");
        gauge.updateFromNetwork(100, 75, 1);
        helper.assertTrue(state.getSignal(helper.getLevel(), absolutePos, Direction.NORTH) == 0,
            "解除过载后弱信号必须清零");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).getValue(RedStoneWireBlock.POWER) == 0,
            "解除过载后必须通知红石粉清零");
        gauge.updateFromNetwork(0, 0, 0);
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), absolutePos) == 0,
            "动力停止后比较器信号必须清零");
        helper.succeed();
    }
}
