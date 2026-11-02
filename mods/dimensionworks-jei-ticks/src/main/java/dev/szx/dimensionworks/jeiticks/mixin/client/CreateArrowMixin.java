package dev.szx.dimensionworks.jeiticks.mixin.client;

import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer;
import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer.Direction;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.gui.AllGuiTextures", remap = false)
public abstract class CreateArrowMixin {
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getHeight();

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$arrow(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        String name = ((Enum<?>) (Object) this).name();
        Direction direction = switch (name) {
            case "JEI_ARROW", "JEI_LONG_ARROW" -> Direction.RIGHT;
            case "JEI_DOWN_ARROW" -> Direction.DOWN;
            default -> null;
        };
        if (direction != null && TickArrowRenderer.draw(graphics, x, y, getWidth(), getHeight(), direction)) {
            ci.cancel();
        }
    }
}
