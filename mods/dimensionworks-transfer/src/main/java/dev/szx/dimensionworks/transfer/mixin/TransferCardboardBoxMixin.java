package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mekanism and CMP use the same unboxing method; deny before it touches any tile NBT. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox", remap = false)
public abstract class TransferCardboardBoxMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$denyUnboxing(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isShiftKeyDown() && TransferBlockPolicy.inTransfer(level)
                && TransferBlockPolicy.forbiddenBoxContents(level, pos)) {
            TransferBlockPolicy.notifyDenied(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lmekanism/common/attachments/BlockData;tryPlaceIntoWorld(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z",
            shift = At.Shift.AFTER, remap = false), remap = false)
    private void dimensionworks$unpackSophisticatedStorage(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (ModList.get().isLoaded("sophisticatedstorage")
                && blockEntity instanceof SophisticatedStoragePackedAccessor accessor) {
            accessor.dimensionworks$setPacked(false);
            blockEntity.setChanged();
        }
    }
}
