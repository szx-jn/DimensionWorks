package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mekanism and CMP use the same unboxing method; deny before it touches any tile NBT. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox", remap = false)
public abstract class TransferCardboardBoxMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$denyUnboxing(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isShiftKeyDown() && TransferBlockPolicy.inTransfer(level)
                && TransferBlockPolicy.forbiddenBoxContents(level, pos)) {
            TransferBlockPolicy.notifyDenied(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
