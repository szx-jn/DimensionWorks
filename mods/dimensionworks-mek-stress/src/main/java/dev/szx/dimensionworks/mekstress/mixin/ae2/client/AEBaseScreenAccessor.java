package dev.szx.dimensionworks.mekstress.mixin.ae2.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AEBaseScreen.class, remap = false)
public interface AEBaseScreenAccessor {

    @Invoker("getGuiLeft")
    int dimensionworks$getGuiLeft();

    @Invoker("getGuiTop")
    int dimensionworks$getGuiTop();

    @Invoker("getStyle")
    ScreenStyle dimensionworks$getStyle();

    @Invoker("getMinecraft")
    Minecraft dimensionworks$getMinecraft();
}
