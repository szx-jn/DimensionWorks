package dev.szx.dimensionworks.cavefactory.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Composes sprites from the active molten animation and crystal overlay on each resource reload. */
@EventBusSubscriber(modid = "dimensionworks_cave_factory", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrystallizedMetalSpriteLoader implements SpriteSource {
    public static final MapCodec<CrystallizedMetalSpriteLoader> CODEC = MapCodec.unit(CrystallizedMetalSpriteLoader::new);
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    @SubscribeEvent public static void register(RegisterSpriteSourceTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "crystallized_metal"), TYPE);
    }
    @Override public SpriteSourceType type() { return TYPE; }

    @Override public void run(ResourceManager resources, SpriteSource.Output output) {
        for (var entry : CaveFactoryClientEvents.MOLTEN_COLORS.entrySet()) {
            for (String kind : new String[]{"still", "flow"}) {
                var name = ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "block/generated/crystallized_molten_" + entry.getKey() + "_" + kind);
                var baseId = ResourceLocation.fromNamespaceAndPath("kubejs", "textures/block/thick_fluid_" + kind + ".png");
                var base = resources.getResource(baseId).orElseThrow(() -> new IllegalStateException("Missing molten animation " + baseId));
                output.add(name, loader -> loader.loadSprite(name, base, (sprite, size, image, metadata) -> {
                    var overlayId = ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "textures/block/crystallized_molten_metal.png");
                    try (var input = resources.getResourceOrThrow(overlayId).open(); var overlay = NativeImage.read(input)) {
                        int tint = entry.getValue();
                        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                            int pixel = image.getPixelRGBA(x, y);
                            int crystal = overlay.getPixelRGBA((x % size.width()) * overlay.getWidth() / size.width(),
                                (y % size.height()) * overlay.getHeight() / size.height());
                            float alpha = (crystal >>> 24) / 255.0F * 0.35F;
                            int red = blend((pixel & 255) * ((tint >> 16) & 255) / 255, crystal & 255, alpha);
                            int green = blend(((pixel >> 8) & 255) * ((tint >> 8) & 255) / 255, (crystal >> 8) & 255, alpha);
                            int blue = blend(((pixel >> 16) & 255) * (tint & 255) / 255, (crystal >> 16) & 255, alpha);
                            image.setPixelRGBA(x, y, (pixel & 0xFF000000) | (blue << 16) | (green << 8) | red);
                        }
                        return new SpriteContents(sprite, size, image, metadata);
                    } catch (java.io.IOException exception) {
                        image.close();
                        throw new IllegalStateException("Cannot compose crystallized molten texture " + name, exception);
                    }
                }));
            }
        }
    }

    private static int blend(int base, int overlay, float alpha) { return Math.round(base * (1 - alpha) + overlay * alpha); }
}
