package dev.szx.dimensionworks.transfer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Registers the user-configurable transfer key. */
@net.neoforged.fml.common.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TransferKeyMappings {
    public static final String CATEGORY = "key.categories.dimensionworks_transfer";
    public static final KeyMapping OPEN_TRANSFER = new KeyMapping(
            "key.dimensionworks_transfer.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY);

    private TransferKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRANSFER);
    }
}
