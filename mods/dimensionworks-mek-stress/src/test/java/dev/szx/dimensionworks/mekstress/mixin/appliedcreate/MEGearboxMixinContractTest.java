package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;

class MEGearboxMixinContractTest {
    @Test
    void replacesCapacitySpeedAndExportTickCalculation() {
        Set<String> injectedTargets = new HashSet<>();
        for (Method method : MEGearboxBlockEntityMixin.class.getDeclaredMethods()) {
            Inject inject = method.getAnnotation(Inject.class);
            if (inject != null) {
                injectedTargets.addAll(Arrays.asList(inject.method()));
            }
        }

        assertTrue(injectedTargets.contains("getGeneratedSpeed"));
        assertTrue(injectedTargets.contains("calculateAddedStressCapacity"));
        assertTrue(injectedTargets.contains("tickExport"));
    }

    @Test
    void doesNotRewriteConfiguredSpeedForThrottling() {
        assertFalse(Arrays.stream(MEGearboxBlockEntityMixin.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().contains("prepareSharedScheduling")));
    }
}
