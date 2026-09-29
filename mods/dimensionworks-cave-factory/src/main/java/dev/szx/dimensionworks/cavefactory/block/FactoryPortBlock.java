package dev.szx.dimensionworks.cavefactory.block;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryPortBlockEntity;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class FactoryPortBlock extends RotatedPillarKineticBlock
    implements IBE<FactoryPortBlockEntity>, IWrenchable {
    public FactoryPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<FactoryPortBlockEntity> getBlockEntityClass() {
        return FactoryPortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FactoryPortBlockEntity> getBlockEntityType() {
        return CFBlockEntities.PORT.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        FactoryPortBlockEntity port = getBlockEntity(level, pos);
        FactoryControllerBlockEntity controller = port == null ? null : port.controller();
        if (controller == null) {
            return InteractionResult.PASS;
        }
        NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, controller, controller.getBlockPos());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        FactoryPortBlockEntity port = getBlockEntity(level, pos);
        FactoryControllerBlockEntity controller = port == null ? null : port.controller();
        Direction side = port == null ? null : port.outwardDirection();
        if (controller == null || side == null || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return controller.cyclePort(side, context.getPlayer())
            ? InteractionResult.SUCCESS
            : InteractionResult.FAIL;
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        net.minecraft.world.level.block.Block block,
        BlockPos sourcePos,
        boolean moving
    ) {
        super.neighborChanged(state, level, pos, block, sourcePos, moving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, port -> {
                FactoryControllerBlockEntity controller = port.controller();
                if (controller != null) {
                    controller.markStructureDirty();
                }
            });
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        FactoryPortBlockEntity port = getBlockEntity(level, pos);
        FactoryControllerBlockEntity controller = port == null ? null : port.controller();
        super.onRemove(state, level, pos, newState, moving);
        if (controller != null) {
            controller.markStructureDirty();
        }
    }
}
