package dev.szx.dimensionworks.ftbchunksnoforceload.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel", remap = false)
abstract class ChunkScreenPanelMixin {
    @Shadow @Final private Set<?> selectedChunks;

    @Inject(
            method = "mouseReleased",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/Panel;mouseReleased(Ldev/ftb/mods/ftblibrary/ui/input/MouseButton;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            remap = false
    )
    private void dimensionworks$disableShiftLeftForceLoad(MouseButton button, CallbackInfo ci) {
        if (!Screen.hasShiftDown() || selectedChunks.isEmpty() || !button.isLeft()) {
            return;
        }

        // Shift+左键是 FTB Chunks 的强加载操作；普通圈地不受影响。
        selectedChunks.clear();
        ci.cancel();
    }

}
