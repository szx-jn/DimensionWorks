package dev.szx.dimensionworks.cavefactory.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterTextureAtlasSpriteLoadersEvent;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.minecraftforge.client.textures.ITextureAtlasSpriteLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Preserves the molten animation, adding the existing crystal texture during sprite baking. */
@Mod.EventBusSubscriber(modid = "dimensionworks_cave_factory", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrystallizedMetalSpriteLoader implements ITextureAtlasSpriteLoader {
    @SubscribeEvent public static void register(RegisterTextureAtlasSpriteLoadersEvent event) {
        event.register("crystallized_metal", new CrystallizedMetalSpriteLoader());
    }

    @Override public SpriteContents loadContents(ResourceLocation name, Resource resource, FrameSize size,
            NativeImage image, AnimationMetadataSection animation, ForgeTextureMetadata metadata) {
        // Placeholder pixels are never displayed: read the active pack's original molten animation.
        image.close();
        var baseId = new ResourceLocation("kubejs", "textures/block/thick_fluid_"
            + (name.getPath().endsWith("_flow") ? "flow" : "still") + ".png");
        try {
            var base = Minecraft.getInstance().getResourceManager().getResourceOrThrow(baseId);
            try (var input = base.open()) { image = NativeImage.read(input); }
            animation = base.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
            size = animation.calculateFrameSize(image.getWidth(), image.getHeight());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Missing original molten animation " + baseId, exception);
        }
        String metal = name.getPath().replace("block/crystallized_molten_", "").replaceFirst("_(still|flow)$", "");
        int tint = CaveFactoryClientEvents.MOLTEN_COLORS.getOrDefault(metal, 0xFFFFFF);
        var overlayId = new ResourceLocation("dimensionworks_cave_factory", "textures/block/crystallized_molten_metal.png");
        try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(overlayId).open();
             var overlay = NativeImage.read(input)) {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int base = image.getPixelRGBA(x, y);
                int crystal = overlay.getPixelRGBA((x % size.width()) * overlay.getWidth() / size.width(),
                    (y % size.height()) * overlay.getHeight() / size.height());
                float alpha = (crystal >>> 24) / 255.0F * 0.35F;
                int red = blend((base & 255) * ((tint >> 16) & 255) / 255, crystal & 255, alpha);
                int green = blend(((base >> 8) & 255) * ((tint >> 8) & 255) / 255, (crystal >> 8) & 255, alpha);
                int blue = blend(((base >> 16) & 255) * (tint & 255) / 255, (crystal >> 16) & 255, alpha);
                image.setPixelRGBA(x, y, (base & 0xFF000000) | (blue << 16) | (green << 8) | red);
            }
        } catch (java.io.IOException exception) {
            image.close();
            throw new IllegalStateException("Cannot compose crystallized molten texture " + name, exception);
        }
        return new SpriteContents(name, size, image, animation, metadata);
    }

    private static int blend(int base, int overlay, float alpha) { return Math.round(base * (1 - alpha) + overlay * alpha); }

    @Override public TextureAtlasSprite makeSprite(ResourceLocation atlas, SpriteContents contents, int width,
            int height, int x, int y, int mipmapLevel) {
        return new TextureAtlasSprite(atlas, contents, width, height, x, y) {};
    }
}
