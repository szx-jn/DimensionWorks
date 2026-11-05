package dev.szx.dimensionworks.transfer.client;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TutorialOverlay {
    private static boolean locked;
    private static int frame;
    private TutorialOverlay() {}
    public static void setLocked(boolean value) { locked = value; }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!locked || Minecraft.getInstance().player == null) return;
        frame++;
        var g = event.getGuiGraphics();
        int width = g.guiWidth(), height = g.guiHeight();
        int pulse = (int)(80 + 60 * Math.sin(frame * .12));
        g.fill(width/2-126,height/2-33,width/2+126,height/2+38,0xD0142535);
        g.fill(width/2-127,height/2-34,width/2+127,height/2-31,0xFF55D7F0);
        g.fill(width/2-127,height/2+37,width/2+127,height/2+39,0xFF55D7F0);
        g.drawCenteredString(Minecraft.getInstance().font,
            Component.translatable("message.dimensionworks_transfer.tutorial_title").withStyle(ChatFormatting.WHITE),
            width/2,height/2-18,0xFFFFFFFF);
        g.drawCenteredString(Minecraft.getInstance().font,
            Component.translatable("message.dimensionworks_transfer.tutorial_action").withStyle(ChatFormatting.WHITE),
            width/2,height/2+7,(pulse<<24)|0x00BFFFEF);
    }
}
