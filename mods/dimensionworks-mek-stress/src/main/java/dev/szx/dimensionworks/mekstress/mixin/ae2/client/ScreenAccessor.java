package dev.szx.dimensionworks.mekstress.mixin.ae2.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenAccessor {

    @Accessor("font")
    Font dimensionworks$getFont();

    @Accessor("renderables")
    List<Renderable> dimensionworks$getRenderables();

    @Accessor("children")
    List<GuiEventListener> dimensionworks$getChildren();

    @Accessor("narratables")
    List<NarratableEntry> dimensionworks$getNarratables();
}
