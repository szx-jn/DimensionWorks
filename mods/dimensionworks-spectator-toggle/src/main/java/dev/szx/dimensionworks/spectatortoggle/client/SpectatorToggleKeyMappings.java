package dev.szx.dimensionworks.spectatortoggle.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = DimensionWorksSpectatorToggle.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class SpectatorToggleKeyMappings {
    public static final KeyMapping TOGGLE_SPECTATOR = new KeyMapping(
            "key.dimensionworks_spectator_toggle.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F4,
            "key.categories.dimensionworks_spectator_toggle");

    private SpectatorToggleKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SPECTATOR);
    }
}
