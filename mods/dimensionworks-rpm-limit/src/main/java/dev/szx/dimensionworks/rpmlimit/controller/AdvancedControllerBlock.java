package dev.szx.dimensionworks.rpmlimit.controller;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;

public final class AdvancedControllerBlock extends SpeedControllerBlock {
    public AdvancedControllerBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntityType<? extends SpeedControllerBlockEntity> getBlockEntityType() { return AdvancedController.ENTITY.get(); }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult placement = super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (placement.consumesAction()) return placement;
        if (player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.mayBuild()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AdvancedControllerEntity entity) {
            int target = entity.targetSpeed.getValue();
            int limit = AdvancedControllerEntity.maximumTarget();
            int effectiveLimit = (int) Math.abs(RpmLimitManager.clamp(entity, limit));
            serverPlayer.openMenu( new SimpleMenuProvider(
                (id, inventory, p) -> new AdvancedControllerMenu(id, inventory, pos, target, limit, effectiveLimit),
                Component.translatable("block.dimensionworks_rpm_limit.advanced_speed_controller")),
                data -> { data.writeBlockPos(pos); data.writeInt(target); data.writeInt(limit); data.writeInt(effectiveLimit); });
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
