package dev.szx.dimensionworks.ftbchunksnoforceload.mixin;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl", remap = false)
abstract class ChunkTeamDataImplMixin {
    @Inject(method = "forceLoad", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$denyForceLoad(CallbackInfoReturnable<ClaimResult> cir) {
        cir.setReturnValue(ClaimResult.StandardProblem.NOT_ENOUGH_POWER);
    }

    @Inject(method = "updateChunkTickets", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$disableTicketRestoration(boolean load, CallbackInfo ci) {
        // Never restore/add FTB Chunks tickets, but allow removal of existing tickets.
        if (load) {
            ci.cancel();
        }
    }
}
