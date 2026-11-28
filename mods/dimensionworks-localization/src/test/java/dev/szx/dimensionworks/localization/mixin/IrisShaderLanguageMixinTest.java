package dev.szx.dimensionworks.localization.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class IrisShaderLanguageMixinTest {
    private static final String TRANSLATION_RESOURCE =
            "/assets/dimensionworks_localization/iris/shaders/lang/complementary_unbound_r5_9_3_zh_cn.lang";

    @Test
    void translationIsAvailableFromMixinClasspathDuringEarlyInitialization() throws IOException {
        try (InputStream input = IrisShaderLanguageMixin.class.getResourceAsStream(TRANSLATION_RESOURCE)) {
            assertNotNull(input, "The shader translation must be bundled beside the localization mod");
            Properties translations = new Properties();
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                translations.load(reader);
            }
            assertEquals("提示信息", translations.getProperty("screen.INFORMATION"));
        }
    }
}
