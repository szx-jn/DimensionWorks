package dev.szx.dimensionworks.mekstress.block;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.blockentity.MatterBallUnpackerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public final class MatterBallUnpackerBlock extends Block implements EntityBlock {
    private final RegistryObject<BlockEntityType<MatterBallUnpackerBlockEntity>> type;

    public MatterBallUnpackerBlock(Properties properties,
        RegistryObject<BlockEntityType<MatterBallUnpackerBlockEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterBallUnpackerBlockEntity(type.get(), pos, state);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(DimensionWorksMekStress.MATTER_BALL.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MatterBallUnpackerBlockEntity unpacker && unpacker.unpack(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer)
            serverPlayer.displayClientMessage(Component.translatable("block.dimensionworks_mek_stress.matter_ball_unpacker.full"), true);
        return InteractionResult.CONSUME;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof MatterBallUnpackerBlockEntity unpacker && !unpacker.isEmpty()) {
            List<ItemStack> packed = unpacker.packContents();
            if (packed != null) drops.addAll(packed);
            else for (ItemStack drop : drops)
                if (drop.is(asItem())) drop.addTagElement("BlockEntityTag", unpacker.saveWithoutMetadata());
        }
        return drops;
    }
}
