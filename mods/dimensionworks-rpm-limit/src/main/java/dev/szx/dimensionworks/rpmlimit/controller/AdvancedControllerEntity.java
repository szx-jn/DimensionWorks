package dev.szx.dimensionworks.rpmlimit.controller;

import java.util.List;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AdvancedControllerEntity extends SpeedControllerBlockEntity {
    public AdvancedControllerEntity(BlockPos pos, BlockState state) { super(AdvancedController.ENTITY.get(), pos, state); }

    public static int maximumTarget() {
        return Math.min(RpmLimitConfig.MAX_RPM.get(), AllConfigs.server().kinetics.maxRotationSpeed.get());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // Keep Create's persistence, synchronization and network rebuild callback, hide its value box.
        targetSpeed.between(-maximumTarget(), maximumTarget());
        targetSpeed.onlyActiveWhen(() -> false);
    }
}
