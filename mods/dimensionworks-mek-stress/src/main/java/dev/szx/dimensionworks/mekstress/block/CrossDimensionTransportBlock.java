package dev.szx.dimensionworks.mekstress.block;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** One-block Create power endpoint. Its shaft axis is shown by Create's horizontal-axis block state. */
public final class CrossDimensionTransportBlock extends HorizontalAxisKineticBlock implements EntityBlock {
    private final int tier;
    private final RegistryObject<BlockEntityType<CrossDimensionTransportBlockEntity>> type;

    public CrossDimensionTransportBlock(int tier, Properties properties,
        RegistryObject<BlockEntityType<CrossDimensionTransportBlockEntity>> type) {
        super(properties);
        if (tier < 1 || tier > 3) throw new IllegalArgumentException("tier");
        this.tier = tier;
        this.type = type;
    }

    public int tier() { return tier; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossDimensionTransportBlockEntity(type.get(), pos, state);
    }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> actualType) {
        if (actualType != type.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CrossDimensionTransportBlockEntity>)
            (world, pos, blockState, entity) -> {
                entity.tick();
                if (world instanceof ServerLevel server) entity.serverTick(server);
            };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player
            && level.getBlockEntity(pos) instanceof CrossDimensionTransportBlockEntity endpoint) {
            endpoint.claimInitialChannel(player);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack heldItem = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof CrossDimensionTransportBlockEntity endpoint) {
            if (heldItem.is(DimensionWorksMekStress.ITEM_CAPACITY_UPGRADE.get())
                || heldItem.is(DimensionWorksMekStress.FLUID_CAPACITY_UPGRADE.get())) {
                endpoint.installCapacityUpgrade(serverPlayer, hand,
                    heldItem.is(DimensionWorksMekStress.FLUID_CAPACITY_UPGRADE.get()));
                return InteractionResult.CONSUME;
            }
            endpoint.openConfiguration(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CrossDimensionTransportBlockEntity endpoint) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) drop.addTagElement("BlockEntityTag", endpoint.saveWithoutMetadata());
            }
        }
        return drops;
    }
}
