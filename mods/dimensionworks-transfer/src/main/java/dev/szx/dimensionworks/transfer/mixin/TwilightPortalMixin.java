package dev.szx.dimensionworks.transfer.mixin;
import dev.szx.dimensionworks.transfer.dispatch.TwilightDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Refuse traditional TF entry before its native transition creates a portal. */
@Mixin(value=twilightforest.world.TFTeleporter.class,remap=false)
public abstract class TwilightPortalMixin {
    @Inject(method="createTransition",at=@At("HEAD"),cancellable=true)
    private static void dimensionworks$blockTraditionalPortal(Entity entity,ServerLevel destination,BlockPos pos,boolean forced,
            CallbackInfoReturnable<DimensionTransition> callback) {
        if(entity instanceof ServerPlayer && destination.dimension().equals(TwilightDestination.KEY))callback.setReturnValue(null);
    }
}
