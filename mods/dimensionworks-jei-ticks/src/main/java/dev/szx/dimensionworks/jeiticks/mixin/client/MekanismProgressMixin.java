package dev.szx.dimensionworks.jeiticks.mixin.client;

import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer;
import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer.Direction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mekanism.client.gui.element.progress.GuiProgress", remap = false)
public abstract class MekanismProgressMixin {
    @Shadow public abstract int getRelativeX();
    @Shadow public abstract int getRelativeY();
    @Shadow protected abstract ResourceLocation getResource();

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$arrow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks,
                                     CallbackInfo ci) {
        String path = getResource().getPath();
        String texture = path.substring(path.lastIndexOf('/') + 1);
        Direction direction = switch (texture) {
            case "bar.png", "large_right.png", "tall_right.png", "right.png", "small_right.png" -> Direction.RIGHT;
            case "large_left.png", "small_left.png" -> Direction.LEFT;
            case "down.png" -> Direction.DOWN;
            default -> null; // Flames, upgrade indicators and bidirectional flows are not timed arrows.
        };
        AbstractWidget widget = (AbstractWidget) (Object) this;
        if (direction != null && TickArrowRenderer.draw(graphics, getRelativeX(), getRelativeY(),
                widget.getWidth(), widget.getHeight(), direction)) {
            ci.cancel();
        }
    }
}
