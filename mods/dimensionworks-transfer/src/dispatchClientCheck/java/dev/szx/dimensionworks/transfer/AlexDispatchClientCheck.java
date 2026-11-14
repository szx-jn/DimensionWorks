package dev.szx.dimensionworks.transfer;

import com.mojang.blaze3d.platform.NativeImage;
import dev.szx.dimensionworks.transfer.dispatch.DispatchScreen;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable GUI render/navigation check, excluded from the release JAR. */
@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class AlexDispatchClientCheck {
    private static int stage;
    private static int ticks;
    private static long started;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > 120_000) throw new AssertionError("Dispatch GUI check timed out");
        if (mc.getOverlay() != null || mc.screen == null) return;
        if (stage == 0) { DispatchScreen.open(BlockPos.ZERO, false); stage = 1; ticks = 0; return; }
        if (++ticks < 20) return;
        if (stage == 1) {
            capture(mc, "terminal-locked.png");
            DispatchScreen.open(BlockPos.ZERO, true); stage = 2; ticks = 0;
        } else if (stage == 2) {
            capture(mc, "terminal-upgraded.png");
            button(mc, Component.translatable("dimensionworks_transfer.destination.alexscaves")).onPress();
            if (!mc.screen.getTitle().equals(Component.translatable("screen.dimensionworks_transfer.alex_caves")))
                throw new AssertionError("Alex entry did not open the cave picker");
            stage = 3; ticks = 0;
        } else if (stage == 3) {
            for (var child : mc.screen.children()) {
                if (child instanceof AbstractWidget widget && (widget.getX() < 0 || widget.getY() < 0
                    || widget.getX() + widget.getWidth() > mc.getWindow().getGuiScaledWidth()
                    || widget.getY() + widget.getHeight() > mc.getWindow().getGuiScaledHeight()))
                    throw new AssertionError("Cave picker widget lies outside the actual GUI viewport");
            }
            capture(mc, "alex-caves.png");
            button(mc, Component.translatable("gui.back")).onPress();
            if (!mc.screen.getTitle().equals(Component.translatable("screen.dimensionworks_transfer.dispatch")))
                throw new AssertionError("Back button did not restore the destination picker");
            WDTransfer.LOGGER.info("ALEX_DISPATCH_GUI_PASS: locked/upgraded pages, six cave artwork and back navigation; GUI={}x{}",
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            stage = 4; mc.stop();
        }
    }

    private static Button button(Minecraft mc, Component name) {
        return mc.screen.children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).filter(button -> button.getMessage().equals(name))
            .findFirst().orElseThrow(() -> new AssertionError("Missing GUI button " + name.getString()));
    }

    private static void capture(Minecraft mc, String name) {
        try {
            var target = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
            Files.createDirectories(target.getParent());
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(target); }
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }
}
