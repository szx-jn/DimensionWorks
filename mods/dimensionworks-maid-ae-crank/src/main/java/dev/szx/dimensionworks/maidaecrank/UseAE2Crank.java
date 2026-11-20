package dev.szx.dimensionworks.maidaecrank;

import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.misc.CrankBlock;
import appeng.blockentity.misc.CrankBlockEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Comparator;

public final class UseAE2Crank extends MaidCheckRateTask {
    private static final int INTERACTION_INTERVAL = 18;
    private static final double REACH_SQR = 16.0;
    private BlockPos crankPos;
    private int interactionCooldown;

    public UseAE2Crank() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        setMaxCheckRate(20);
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }

        BlockPos found = findUsableCrank(level, maid);
        if (found == null) {
            crankPos = null;
            return false;
        }

        crankPos = found;
        BlockPos workPos = getWorkPosition(level, crankPos);
        if (maid.position().distanceToSqr(Vec3.atCenterOf(workPos)) > REACH_SQR) {
            BehaviorUtils.setWalkAndLookTargetMemories(maid, workPos, 0.5F, 1);
            setNextCheckTickCount(5);
            return false;
        }

        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        if (crankPos != null) {
            maid.getLookControl().setLookAt(Vec3.atCenterOf(crankPos));
        }
        interactionCooldown = 0;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        if (maid.getScheduleDetail() != Activity.WORK || crankPos == null) {
            return false;
        }
        if (!level.hasChunkAt(crankPos) || !(level.getBlockEntity(crankPos) instanceof CrankBlockEntity)) {
            return false;
        }
        if (maid.position().distanceToSqr(Vec3.atCenterOf(getWorkPosition(level, crankPos))) > REACH_SQR) {
            return false;
        }
        return canTurn(level, crankPos);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        if (crankPos == null) {
            return;
        }

        maid.getLookControl().setLookAt(Vec3.atCenterOf(crankPos));
        if (interactionCooldown > 0) {
            interactionCooldown--;
            return;
        }

        if (level.getBlockEntity(crankPos) instanceof CrankBlockEntity crank && canTurn(level, crankPos)) {
            // Never invoke useWithoutItem with a fake player: AE2 intentionally drops the crank for FakePlayer.
            crank.power();
            maid.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            interactionCooldown = INTERACTION_INTERVAL;
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        crankPos = null;
        interactionCooldown = 0;
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private BlockPos findUsableCrank(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.hasRestriction() ? maid.getRestrictCenter() : maid.blockPosition();
        int radius = maid.hasRestriction()
                ? Math.max(4, Math.min(8, (int) maid.getRestrictRadius()))
                : 8;
        BlockPos min = center.offset(-radius, -4, -radius);
        BlockPos max = center.offset(radius, 4, radius);

        return BlockPos.betweenClosedStream(min, max)
                .map(BlockPos::immutable)
                .filter(level::hasChunkAt)
                .filter(pos -> level.getBlockEntity(pos) instanceof CrankBlockEntity)
                .filter(pos -> canTurn(level, pos))
                .min(Comparator.comparingDouble(pos -> maid.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .orElse(null);
    }

    private boolean canTurn(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CrankBlock crankBlock)) {
            return false;
        }
        ICrankable crankable = crankBlock.getCrankable(state, level, pos);
        return crankable != null && crankable.canTurn();
    }

    private BlockPos getWorkPosition(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CrankBlock crankBlock) {
            Direction facing = OrientationStrategies.facing().getFacing(state);
            return pos.relative(facing);
        }
        return pos;
    }
}
