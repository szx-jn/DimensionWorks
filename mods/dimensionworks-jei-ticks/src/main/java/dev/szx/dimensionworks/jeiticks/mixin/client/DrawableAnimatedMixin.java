package dev.szx.dimensionworks.jeiticks.mixin.client;

import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer;
import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer.Direction;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.common.gui.elements.DrawableAnimated", remap = false)
public abstract class DrawableAnimatedMixin {
    @Shadow @Final private IDrawableStatic drawable;
    @Shadow @Final private StartDirection startDirection;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$arrow(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        Direction direction;
        if (startDirection == StartDirection.LEFT || startDirection == StartDirection.RIGHT) {
            // StartDirection describes the reveal edge, not the arrow head direction.
            direction = Direction.RIGHT;
        } else if (drawable.getWidth() == 7 && drawable.getHeight() == 27
                && startDirection == StartDirection.TOP) {
            direction = Direction.DOWN; // Vanilla brewing arrow; leave flames/bubbles alone.
        } else {
            return;
        }
        if (TickArrowRenderer.draw(graphics, x, y, drawable.getWidth(), drawable.getHeight(), direction)) {
            ci.cancel();
        }
    }
}
