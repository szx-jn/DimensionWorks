package dev.szx.dimensionworks.rpmlimit.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DimensionWorksRpmLimit.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ControllerClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(AdvancedController.MENU.get(), ControllerScreen::new));
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AdvancedController.ENTITY.get(), Renderer::new);
    }
    private static final class Renderer extends SpeedControllerRenderer {
        Renderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected void renderSafe(SpeedControllerBlockEntity entity, float partialTicks, PoseStack pose,
                                             MultiBufferSource buffer, int light, int overlay) {
            super.renderSafe(entity, partialTicks, pose, buffer, light, overlay);
            // This type uses the inherited renderer, without a Flywheel visual. Render its shaft in both modes.
            if (VisualizationManager.supportsVisualization(entity.getLevel())) {
                var shaft = CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK,
                    KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(entity)));
                KineticBlockEntityRenderer.renderRotatingBuffer(entity, shaft, pose,
                    buffer.getBuffer(Sheets.solidBlockSheet()), light);
            }
        }
    }
}
