package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class PackedPlayerPushMixin {
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$keepParcelOnChain(double x, double z, CallbackInfo ci) {
        if (PackedPlayerTransport.ignoresBlocks((LocalPlayer) (Object) this)) ci.cancel();
    }
}
