package dev.szx.dimensionworks.mekstress.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.TransportNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@net.neoforged.fml.common.EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID, value = Dist.CLIENT)
public final class CloudDiskKeyEvents {
    public static final KeyMapping OPEN_CLOUD = new KeyMapping("key.dimensionworks_mek_stress.cloud_disk",
        KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.dimensionworks_mek_stress");
    private CloudDiskKeyEvents() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        while (OPEN_CLOUD.consumeClick()) TransportNetwork.openCloudDisk();
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN_CLOUD); }
    }
}
