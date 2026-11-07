package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.dispatch.TwilightDestination;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block the TF portal teleporter without blocking unlocked command travel. */
@Mixin(ServerPlayer.class)
public abstract class TwilightPortalMixin {
    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;", remap = false, at = @At("HEAD"), cancellable = true)
    private void dimensionworks$blockTraditionalPortal(ServerLevel destination, ITeleporter teleporter,
                                                      CallbackInfoReturnable<Entity> cir) {
        if (!destination.dimension().equals(TwilightDestination.KEY)) return;
        for (Class<?> type = teleporter.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("twilightforest.world.TFTeleporter")) {
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
