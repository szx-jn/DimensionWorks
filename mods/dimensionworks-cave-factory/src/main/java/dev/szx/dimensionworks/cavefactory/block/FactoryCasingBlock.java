package dev.szx.dimensionworks.cavefactory.block;

import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class FactoryCasingBlock extends Block {
    public FactoryCasingBlock(Properties properties) {
        super(properties);
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
            return findController(level, pos) == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        FactoryControllerBlockEntity controller = findController(level, pos);
        if (controller == null) {
            return InteractionResult.PASS;
        }
        NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, controller, controller.getBlockPos());
        return InteractionResult.CONSUME;
    }

    private static FactoryControllerBlockEntity findController(Level level, BlockPos shellPos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos candidate = shellPos.offset(x, y, z);
                    if (level.getBlockEntity(candidate) instanceof FactoryControllerBlockEntity controller) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }
}
