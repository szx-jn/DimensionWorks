package dev.szx.dimensionworks.localization.mixin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.LanguageMap", remap = false)
public abstract class IrisShaderLanguageMixin {
    @Unique
    private static final Logger DIMENSIONWORKS$LOGGER =
            LoggerFactory.getLogger("DimensionWorks Localization");

    @Unique
    private static final ResourceLocation DIMENSIONWORKS$TRANSLATION_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    "dimensionworks_localization",
                    "iris/shaders/lang/complementary_unbound_r5_9_3_zh_cn.lang");

    @Shadow
    @Final
    private Map<String, Map<String, String>> translationMaps;

    @Inject(method = "<init>(Ljava/nio/file/Path;)V", at = @At("TAIL"), remap = false)
    private void dimensionworks$addComplementaryTranslations(Path shaderRoot, CallbackInfo callback) {
        String shaderUri = shaderRoot.toUri().toString().toLowerCase(java.util.Locale.ROOT);
        if (!shaderUri.contains("complementaryunbound") || !shaderUri.contains("r5.9.3")) {
            return;
        }

        try {
            Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(DIMENSIONWORKS$TRANSLATION_RESOURCE)
                    .orElseThrow(() -> new IOException("Bundled Complementary Unbound zh_cn translation not found"));
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            Map<String, String> translations = new HashMap<>();
            properties.forEach((key, value) -> translations.put(key.toString(), value.toString()));
            translationMaps.put("zh_cn", Map.copyOf(translations));
        } catch (IOException exception) {
            DIMENSIONWORKS$LOGGER.error(
                    "Could not load Complementary Unbound zh_cn translation; continuing shaderpack loading",
                    exception);
        }
    }
}
