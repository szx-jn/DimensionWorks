package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;

class MachineEnergyContainerMixinTest {

    @Test
    void overridesTheFullVirtualEnergyContract() {
        Set<String> injectedTargets = new HashSet<>();
        for (Method method : MachineEnergyContainerMixin.class.getDeclaredMethods()) {
            Inject inject = method.getAnnotation(Inject.class);
            if (inject != null) {
                injectedTargets.addAll(Arrays.asList(inject.method()));
            }
        }

        assertTrue(injectedTargets.contains("getEnergy"));
        assertTrue(injectedTargets.contains("isEmpty"));
        assertTrue(injectedTargets.contains("extract"));
        assertTrue(injectedTargets.contains("insert"));
    }
}
