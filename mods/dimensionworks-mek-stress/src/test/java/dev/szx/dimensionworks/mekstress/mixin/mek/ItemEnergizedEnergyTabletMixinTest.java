package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;

class ItemEnergizedEnergyTabletMixinTest {

    @Test
    void replacesEnergyCapabilityAndCreativeVariant() {
        Set<String> injectedTargets = new HashSet<>();
        for (Method method : ItemEnergizedEnergyTabletMixin.class.getDeclaredMethods()) {
            Inject inject = method.getAnnotation(Inject.class);
            if (inject != null) {
                injectedTargets.addAll(Arrays.asList(inject.method()));
            }
        }

        assertTrue(injectedTargets.contains("gatherCapabilities"));
        assertTrue(injectedTargets.contains("addItems"));
    }
}
