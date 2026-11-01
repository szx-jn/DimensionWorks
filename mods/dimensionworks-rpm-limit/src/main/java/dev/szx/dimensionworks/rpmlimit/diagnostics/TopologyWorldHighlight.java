package dev.szx.dimensionworks.rpmlimit.diagnostics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders a client-only outline for the last topology node selected for highlighting. */
@Mod.EventBusSubscriber(modid = "dimensionworks_rpm_limit", bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class TopologyWorldHighlight {
    private static BlockPos position;
    private static ClientLevel selectedLevel;

    private TopologyWorldHighlight() {}

    public static boolean isHighlighted(BlockPos target) {
        Minecraft minecraft = Minecraft.getInstance();
        return position != null && position.equals(target) && minecraft.level != null
            && selectedLevel == minecraft.level;
    }

    public static void toggle(BlockPos target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (isHighlighted(target)) {
            clear();
            return;
        }
        position = target.immutable();
        selectedLevel = minecraft.level;
    }

    public static void clear() {
        position = null;
        selectedLevel = null;
    }

    @SubscribeEvent
    public static void clearWhenLeavingLevel(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && position != null
            && Minecraft.getInstance().level != selectedLevel) clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || position == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || selectedLevel != minecraft.level
            || !minecraft.level.hasChunkAt(position)) return;

        Vec3 camera = event.getCamera().getPosition();
        AABB outline = new AABB(position).inflate(0.035);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderType lines = RenderType.lines();
        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(lines);
        LevelRenderer.renderLineBox(pose, consumer, outline, 1.0F, 0.82F, 0.12F, 1.0F);
        minecraft.renderBuffers().bufferSource().endBatch(lines);
        pose.popPose();
    }
}
