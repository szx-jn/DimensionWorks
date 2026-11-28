package dev.szx.dimensionworks.shortcutpanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.szx.dimensionworks.shortcutpanel.DimensionWorksShortcutPanel;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ShortcutPanelKeyMappings {
    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.dimensionworks_shortcut_panel.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F12,
            "key.categories.dimensionworks_shortcut_panel");

    private ShortcutPanelKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PANEL);
    }
}
