package dev.szx.dimensionworks.mekstress.block;

import appeng.block.AEBaseEntityBlock;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class MemoryDriveBlock extends AEBaseEntityBlock<MemoryDriveBlockEntity> {
    private final MemoryTier tier;

    public MemoryDriveBlock(MemoryTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public MemoryTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MemoryDriveBlockEntity(DimensionWorksMekStress.MEMORY_DRIVE_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DimensionWorksMekStress.MEMORY_DRIVE_BLOCK_ENTITY.get()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<MemoryDriveBlockEntity>) (lvl, pos, st, be) -> {
                if (!lvl.isClientSide) {
                    MemoryDriveBlockEntity drive = be;
                    if (lvl.getGameTime() % 20L == 0L) {
                        drive.refreshCards();
                    }
                }
            }
            : null;
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof MemoryDriveBlockEntity drive) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inventory, ignored) -> new MemoryDriveMenu(id, inventory, drive),
                Component.translatable("block.dimensionworks_mek_stress.memory_drive_ddr" + tier.index())
            ), buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof MemoryDriveBlockEntity drive) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    drop.addTagElement("BlockEntityTag", drive.saveWithoutMetadata());
                }
            }
        }
        return drops;
    }
}
