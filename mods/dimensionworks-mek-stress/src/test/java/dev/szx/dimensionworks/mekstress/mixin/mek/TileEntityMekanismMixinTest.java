package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import mekanism.common.tile.base.TileEntityMekanism;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;

class TileEntityMekanismMixinTest {
    @Test
    void everyInjectionTargetsADeclaredMekanismMethod() {
        for (Method handler : TileEntityMekanismMixin.class.getDeclaredMethods()) {
            Inject inject = handler.getAnnotation(Inject.class);
            if (inject == null) {
                continue;
            }

            assertTrue(inject.method().length > 0, () -> "Mixin target must not be empty: " + handler.getName());
            for (String target : inject.method()) {
                assertDoesNotThrow(
                    () -> TileEntityMekanism.class.getDeclaredMethod(target),
                    () -> "Mixin target must be declared by TileEntityMekanism: " + target
                );
            }
        }
    }
}
