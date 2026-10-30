package dev.szx.dimensionworks.rpmlimit.controller;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class AdvancedControllerBlock extends SpeedControllerBlock {
    public AdvancedControllerBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntityType<? extends SpeedControllerBlockEntity> getBlockEntityType() { return AdvancedController.ENTITY.get(); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult placement = super.use(state, level, pos, player, hand, hit);
        if (placement.consumesAction()) return placement;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AdvancedControllerEntity entity) {
            int target = entity.targetSpeed.getValue();
            int limit = AdvancedControllerEntity.maximumTarget();
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inventory, p) -> new AdvancedControllerMenu(id, inventory, pos, target, limit),
                Component.translatable("block.dimensionworks_rpm_limit.advanced_speed_controller")),
                data -> { data.writeBlockPos(pos); data.writeInt(target); data.writeInt(limit); });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
