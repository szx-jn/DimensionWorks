package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import mekanism.common.tile.base.TileEntityMekanism;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

class TileEntityMekanismMixinTest {
    @Test
    void everyInjectionTargetsADeclaredMekanismMethod() {
        Method[] targets = TileEntityMekanism.class.getDeclaredMethods();
        for (Method handler : TileEntityMekanismMixin.class.getDeclaredMethods()) {
            Inject inject = handler.getAnnotation(Inject.class);
            if (inject == null) {
                continue;
            }

            assertTrue(inject.method().length > 0, () -> "Mixin target must not be empty: " + handler.getName());
            for (String target : inject.method()) {
                assertTrue(Arrays.stream(targets).anyMatch(method -> method.getName().equals(target)),
                    () -> "Mixin target must be declared by TileEntityMekanism: " + target);
            }
        }
    }

    @Test
    void scopesProductionMultiplierAcrossTheWholeServerTick() {
        boolean pushAtHead = false;
        boolean popAtReturn = false;

        for (Method handler : TileEntityMekanismMixin.class.getDeclaredMethods()) {
            Inject inject = handler.getAnnotation(Inject.class);
            if (inject == null || Arrays.stream(inject.method()).noneMatch("tickServer"::equals)) {
                continue;
            }

            assertTrue(Modifier.isStatic(handler.getModifiers()),
                () -> "Static tickServer injection must be static: " + handler.getName());
            boolean atHead = Arrays.stream(inject.at()).anyMatch(at -> "HEAD".equals(at.value()));
            boolean atReturn = Arrays.stream(inject.at()).anyMatch(at -> "RETURN".equals(at.value()));
            if ("dimensionworks$pushProductionContext".equals(handler.getName())) {
                pushAtHead = atHead;
            } else if ("dimensionworks$popProductionContext".equals(handler.getName())) {
                popAtReturn = atReturn;
            }
        }

        assertTrue(pushAtHead, "Production multiplier must be pushed at tickServer HEAD");
        assertTrue(popAtReturn, "Production multiplier must be restored at tickServer RETURN");
    }

    @Test
    void removesVirtualMembersWhenTheMachineIsRemovedOrUnloaded() {
        boolean found = false;
        for (Method handler : TileEntityMekanismMixin.class.getDeclaredMethods()) {
            Inject inject = handler.getAnnotation(Inject.class);
            if (inject == null) {
                continue;
            }
            for (String target : inject.method()) {
                if ("setRemoved".equals(target)) {
                    found = true;
                }
            }
        }
        assertTrue(found, "setRemoved must detach the machine's virtual kinetic member");
    }
}
