package dev.szx.dimensionworks.stressgaugealarm.mixin;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Limit redstone queries to Create gauges rather than every Minecraft block. */
@Mixin(value = GaugeBlock.class, remap = false)
public abstract class StressGaugeBlockMixin extends DirectionalAxisKineticBlock {
    @Shadow protected GaugeBlock.Type type;

    protected StressGaugeBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return type == GaugeBlock.Type.STRESS || super.isSignalSource(state);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof StressGaugeBlockEntity gauge && gauge.isOverStressed()) {
            return 15;
        }
        return super.getSignal(state, level, pos, direction);
    }
}
