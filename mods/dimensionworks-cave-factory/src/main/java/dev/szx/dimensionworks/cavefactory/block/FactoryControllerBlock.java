package dev.szx.dimensionworks.cavefactory.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FactoryControllerBlock extends BaseEntityBlock
    implements IBE<FactoryControllerBlockEntity>, IWrenchable {
    private final MachineType machineType;

    public FactoryControllerBlock(MachineType machineType, Properties properties) {
        super(properties);
        this.machineType = machineType;
    }

    public MachineType machineType() {
        return machineType;
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
        FactoryControllerBlockEntity controller = getBlockEntity(level, pos);
        if (controller == null) {
            return InteractionResult.PASS;
        }
        NetworkHooks.openScreen((ServerPlayer) player, controller, controller.getBlockPos());
        return InteractionResult.CONSUME;
    }

    @Override
    public Class<FactoryControllerBlockEntity> getBlockEntityClass() {
        return FactoryControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FactoryControllerBlockEntity> getBlockEntityType() {
        return CFBlockEntities.CONTROLLER.get();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof net.minecraft.world.entity.player.Player player) {
            withBlockEntityDo(level, pos, controller -> controller.setOwner(player));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, FactoryControllerBlockEntity::onRemovedByBlock);
        }
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moving);
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
            withBlockEntityDo(level, pos, FactoryControllerBlockEntity::markStructureDirty);
        }
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        FactoryControllerBlockEntity controller = getBlockEntity(context.getLevel(), context.getClickedPos());
        if (controller != null && controller.hasStoredFluid()) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("message.dimensionworks_cave_factory.fluid_move_denied"),
                    true
                );
            }
            return InteractionResult.FAIL;
        }
        return IWrenchable.super.onSneakWrenched(state, context);
    }
}
