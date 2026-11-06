package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class TransferBlockPlacementMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$denyPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (TransferBlockPolicy.inTransfer(context.getLevel())
                && TransferBlockPolicy.forbidden(((BlockItem) (Object) this).getBlock().defaultBlockState())) {
            TransferBlockPolicy.notifyDenied(context.getPlayer());
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

@Mixin(Level.class)
abstract class TransferLevelPlacementMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"), cancellable = true)
    private void dimensionworks$denyWrite(BlockPos pos, BlockState state, int flags, int recursion,
            CallbackInfoReturnable<Boolean> cir) {
        if (TransferBlockPolicy.deniesWrite((Level) (Object) this, pos, state)) cir.setReturnValue(false);
    }
}

/** Covers tools and automations that write a full chunk directly. */
@Mixin(LevelChunk.class)
abstract class TransferChunkPlacementMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$denyWrite(BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> cir) {
        var chunk = (LevelChunk) (Object) this;
        if (TransferBlockPolicy.deniesWrite(chunk.getLevel(), pos, state)) cir.setReturnValue(null);
    }
}
