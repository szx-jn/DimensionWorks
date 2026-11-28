package dev.szx.dimensionworks.shortcutpanel;

import dev.szx.dimensionworks.shortcutpanel.client.ShortcutPanelKeyMappings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(DimensionWorksShortcutPanel.MOD_ID)
public final class DimensionWorksShortcutPanel {
    public static final String MOD_ID = "dimensionworks_shortcut_panel";

    public DimensionWorksShortcutPanel(IEventBus modBus) {
        modBus.addListener(ShortcutPanelKeyMappings::register);
    }
}
