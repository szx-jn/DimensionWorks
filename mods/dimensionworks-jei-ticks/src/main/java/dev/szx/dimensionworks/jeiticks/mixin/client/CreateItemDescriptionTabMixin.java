package dev.szx.dimensionworks.jeiticks.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.item.ItemDescription", remap = false)
public abstract class CreateItemDescriptionTabMixin {
    @Redirect(
        method = "getCurrentLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z"
        )
    )
    private boolean dimensionworks$useTabForDetails() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB);
    }
}
