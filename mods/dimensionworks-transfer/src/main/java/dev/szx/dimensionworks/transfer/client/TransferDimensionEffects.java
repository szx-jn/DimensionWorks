package dev.szx.dimensionworks.transfer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Client look of {@code dw:transfer}: no sky, no clouds, no weather, and a permanently
 * bright lightmap of the same intensity the overworld reaches at noon.
 */
@OnlyIn(Dist.CLIENT)
public final class TransferDimensionEffects extends DimensionSpecialEffects {
    public TransferDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                            Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }
}
