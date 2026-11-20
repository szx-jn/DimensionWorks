package dev.szx.dimensionworks.ftbchunksnoforceload.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel$ChunkButton", remap = false)
abstract class ChunkButtonMixin {
    @Redirect(
            method = {"drawBackground", "lambda$addMouseOverText$4", "mouseScrolled"},
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbchunks/client/map/MapChunk;getForceLoadedDate()Ljava/util/Optional;"
            ),
            require = 1
    )
    private Optional<?> dimensionworks$hideForceLoadStatus(@Coerce Object chunk) {
        return Optional.empty();
    }
}
