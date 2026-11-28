package dev.szx.dimensionworks.shortcutpanel.client;

import dev.szx.dimensionworks.shortcutpanel.DimensionWorksShortcutPanel;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DimensionWorksShortcutPanel.MOD_ID, value = Dist.CLIENT)
public final class ShortcutPanelKeyHandler {
    private ShortcutPanelKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ShortcutPanelKeyMappings.OPEN_PANEL.consumeClick()) {
            if (!(minecraft.screen instanceof UnboundShortcutScreen)) {
                minecraft.setScreen(new UnboundShortcutScreen());
            }
        }
    }
}
