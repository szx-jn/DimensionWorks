package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.parts.PartPlacement", remap = false)
public abstract class TransferAePartPlacementMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$denyPlayerPart(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (TransferBlockPolicy.inTransfer(context.getLevel())) {
            TransferBlockPolicy.notifyDenied(context.getPlayer());
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "placePart", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$denyDirectPart(Player player, Level level, @Coerce Object partItem,
            CompoundTag settings, BlockPos pos, Direction side, CallbackInfoReturnable<Object> cir) {
        if (TransferBlockPolicy.inTransfer(level)) {
            TransferBlockPolicy.notifyDenied(player);
            cir.setReturnValue(null);
        }
    }
}
