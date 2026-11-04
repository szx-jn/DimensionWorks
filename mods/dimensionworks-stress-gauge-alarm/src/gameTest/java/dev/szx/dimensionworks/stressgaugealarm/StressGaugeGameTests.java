package dev.szx.dimensionworks.stressgaugealarm;

import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
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
        BlockState state = ForgeRegistries.BLOCKS
            .getValue(new ResourceLocation("create", "stressometer")).defaultBlockState();
        helper.setBlock(pos, state);
        StressGaugeBlockEntity gauge = (StressGaugeBlockEntity) helper.getBlockEntity(pos);

        gauge.updateFromNetwork(100, 50, 1);
        helper.assertTrue(Math.abs(gauge.dialTarget - 0.5F) < 0.001F,
            "Create stressometer should use 50% network utilization, got " + gauge.dialTarget);
        int analog = ((GaugeBlock) state.getBlock()).getAnalogOutputSignal(state, helper.getLevel(), absolutePos);
        helper.assertTrue(analog == 7, "50% stress should produce analog strength 7, got " + analog);

        gauge.updateFromNetwork(100, 125, 1);
        int alarm = state.getBlock().getSignal(state, helper.getLevel(), absolutePos, Direction.NORTH);
        helper.assertTrue(gauge.isOverStressed(), "125 SU on 100 SU capacity should be overstressed");
        helper.assertTrue(alarm == 15, "overstressed gauge should emit full redstone, got " + alarm);
        helper.succeed();
    }
}
