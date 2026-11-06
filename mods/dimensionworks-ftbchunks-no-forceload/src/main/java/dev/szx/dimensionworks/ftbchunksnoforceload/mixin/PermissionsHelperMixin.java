// SPDX-License-Identifier: MIT
package dev.szx.dimensionworks.ftbchunksnoforceload.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.integration.PermissionsHelper", remap = false)
abstract class PermissionsHelperMixin {
    @Inject(
            method = "getChunkLoadOffline(Lnet/minecraft/server/level/ServerPlayer;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void dimensionworks$denyOfflineForceLoadPermission(CallbackInfoReturnable<Boolean> cir) {
        // FTB Chunks force-loading is disabled, so logout must not query a closed permission provider.
        cir.setReturnValue(false);
    }
}
