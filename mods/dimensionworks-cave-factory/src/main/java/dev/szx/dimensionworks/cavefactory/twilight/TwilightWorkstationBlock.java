package dev.szx.dimensionworks.cavefactory.twilight;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;

public final class TwilightWorkstationBlock extends HorizontalAxisKineticBlock
    implements IBE<TwilightWorkstationBlockEntity>, IWrenchable {
    private final boolean biological;
    public TwilightWorkstationBlock(boolean biological, Properties properties) {
        super(properties);
        this.biological = biological;
    }
    public boolean biological() { return biological; }
    @Override public Class<TwilightWorkstationBlockEntity> getBlockEntityClass() { return TwilightWorkstationBlockEntity.class; }
    @Override public BlockEntityType<? extends TwilightWorkstationBlockEntity> getBlockEntityType() {
        return TwilightIndustry.BLOCK_ENTITY.get();
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        TwilightWorkstationBlockEntity machine = getBlockEntity(level, pos);
        if (machine == null) return InteractionResult.PASS;
        if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()) {
            boolean filling = FluidUtil.getFluidContained(player.getItemInHand(hand)).isPresent();
            if (FluidUtil.interactWithFluidHandler(player, hand, filling ? machine.waterTank() : machine.manualTank()))
                return InteractionResult.CONSUME;
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, machine, pos);
        return InteractionResult.CONSUME;
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof TwilightWorkstationBlockEntity machine) {
            for (ItemStack drop : drops) if (drop.is(asItem()))
                drop.addTagElement("BlockEntityTag", machine.portableStorage());
        }
        return drops;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) withBlockEntityDo(level, pos, machine -> machine.releaseWorkers(0));
        super.onRemove(state, level, pos, next, moving);
    }
}
