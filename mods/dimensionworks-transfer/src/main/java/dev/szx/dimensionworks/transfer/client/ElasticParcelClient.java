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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
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

    public static void launch(UUID uuid, Vec3 velocity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(uuid)) return;
        mc.player.setDeltaMovement(velocity);
    }

    private static void reset() { charge = new ElasticCharge(); pressed = false; }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {

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
    public static void overlay(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR) || mc.player == null
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
            int amount = Math.max(0, Math.min(40, charge.ticks() * 120 / ElasticCharge.FULL_TICKS - segment * 40));
            g.fill(x, bottom - 7, x + amount, bottom - 1, full ? 0xFFFFD45B : colors[segment]);
        }
    }

    /** Reuse the exact CMP package model and motion; only palette and opacity change. */
    public static MultiBufferSource slimeBuffers(MultiBufferSource source) {
        return ignored -> new SlimeColor(source.getBuffer(RenderType.translucent()));
    }

    private static final class SlimeColor implements VertexConsumer {
        private final VertexConsumer delegate;
        private SlimeColor(VertexConsumer delegate) { this.delegate = delegate; }
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            int shade = (r + g + b) / 3;
            delegate.setColor((int)(shade * 0.58), Math.min(255, (int)(shade * 1.15)), (int)(shade * 0.42), (int)(a * 0.88));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }

    }
}
