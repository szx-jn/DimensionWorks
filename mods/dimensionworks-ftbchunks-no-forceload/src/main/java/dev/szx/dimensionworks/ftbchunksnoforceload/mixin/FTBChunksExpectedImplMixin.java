package dev.szx.dimensionworks.ftbchunksnoforceload.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.neoforge.FTBChunksExpectedImpl", remap = false)
abstract class FTBChunksExpectedImplMixin {
    @Inject(method = "addChunkToForceLoaded", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$denyFtbChunksTicket(
            ServerLevel level,
            String modId,
            UUID owner,
            int chunkX,
            int chunkZ,
            boolean add,
            CallbackInfo ci
    ) {
        // FTB Chunks' own claim tickets are blocked; other mods' ticket namespaces remain untouched.
        if (add && "ftbchunks".equals(modId)) {
            ci.cancel();
        }
    }
}
