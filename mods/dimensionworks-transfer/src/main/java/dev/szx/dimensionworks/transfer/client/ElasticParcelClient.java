package dev.szx.dimensionworks.transfer.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.elastic.ElasticCharge;
import dev.szx.dimensionworks.transfer.elastic.ElasticNetwork;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class ElasticParcelClient {
    private static ElasticCharge charge = new ElasticCharge();
    private static boolean pressed;
    private static Player localPlayer;
    private static ClientLevel level;

    private ElasticParcelClient() {}

    public static void receive(int id, UUID uuid, boolean elastic) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(id) instanceof Player player) || !player.getUUID().equals(uuid)) return;
        ElasticParcelState.setElastic(player, elastic);
        if (!elastic && player == mc.player) reset();
    }

    private static void reset() { charge = new ElasticCharge(); pressed = false; }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (localPlayer != mc.player || level != mc.level) {
            reset();
            localPlayer = mc.player;
            level = mc.level;
        }
        if (mc.player == null || !ElasticParcelState.isElastic(mc.player)) { reset(); return; }
        if (mc.screen != null || mc.isPaused() || TutorialOverlay.isLocked()) {
            if (pressed) ElasticNetwork.sendInput(false, mc.player.getXRot(), mc.player.getYRot(), true);
            reset();
            return;
        }
        boolean down = mc.options.keyShift.isDown();
        if (pressed != down) {
            ElasticNetwork.sendInput(down, mc.player.getXRot(), mc.player.getYRot(), false);
            charge.input(down);
            pressed = down;
        }
        charge.tick(ElasticParcelState.canCharge(mc.player));
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) || mc.player == null
            || !ElasticParcelState.isElastic(mc.player) || charge.ticks() == 0) return;
        var g = event.getGuiGraphics();
        int left = g.guiWidth() / 2 - 62;
        int bottom = g.guiHeight() - 22 - 20;
        boolean full = charge.ticks() == ElasticCharge.FULL_TICKS;
        int[] colors = {0xFF71CF52, 0xFFEFA33F, 0xFFEA5546};
        for (int segment = 0; segment < 3; segment++) {
            int x = left + segment * 42;
            g.fill(x - 1, bottom - 8, x + 41, bottom, 0xC0162414);
            g.fill(x, bottom - 7, x + 40, bottom - 1, 0xB03A4235);
            int amount = Math.max(0, Math.min(20, charge.ticks() - segment * 20));
            g.fill(x, bottom - 7, x + amount * 2, bottom - 1, full ? 0xFFFFD45B : colors[segment]);
        }
    }

    /** Reuse the exact CMP package model and motion; only palette and opacity change. */
    public static MultiBufferSource slimeBuffers(MultiBufferSource source) {
        return ignored -> new SlimeColor(source.getBuffer(RenderType.translucent()));
    }

    private static final class SlimeColor implements VertexConsumer {
        private final VertexConsumer delegate;
        private SlimeColor(VertexConsumer delegate) { this.delegate = delegate; }
        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            int shade = (r + g + b) / 3;
            delegate.color((int)(shade * 0.58), Math.min(255, (int)(shade * 1.15)), (int)(shade * 0.42), (int)(a * 0.88));
            return this;
        }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) {
            int shade = (r + g + b) / 3;
            delegate.defaultColor((int)(shade * 0.58), Math.min(255, (int)(shade * 1.15)), (int)(shade * 0.42), (int)(a * 0.88));
        }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
