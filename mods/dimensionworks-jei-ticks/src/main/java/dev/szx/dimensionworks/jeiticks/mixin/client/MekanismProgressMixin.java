package dev.szx.dimensionworks.jeiticks.mixin.client;

import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer;
import dev.szx.dimensionworks.jeiticks.client.TickArrowRenderer.Direction;
import net.minecraft.client.gui.GuiGraphics;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiTexturedElement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mekanism.client.gui.element.progress.GuiProgress", remap = false)
public abstract class MekanismProgressMixin extends GuiTexturedElement {
    // These accessors belong to GuiElement/GuiTexturedElement, not GuiProgress.
    // Inherit them normally: Mixin cannot resolve inherited methods as @Shadow here.
    protected MekanismProgressMixin(ResourceLocation resource, IGuiWrapper gui,
                                    int x, int y, int width, int height) {
        super(resource, gui, x, y, width, height);
    }

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
        if (direction != null && TickArrowRenderer.draw(graphics, getRelativeX(), getRelativeY(),
                getWidth(), getHeight(), direction)) {
            ci.cancel();
        }
    }
}
