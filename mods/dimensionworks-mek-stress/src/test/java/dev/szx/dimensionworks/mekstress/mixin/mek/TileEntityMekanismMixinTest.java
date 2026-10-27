package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import mekanism.common.tile.base.TileEntityMekanism;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    void schedulesVirtualMachineOverridesAtTheStaticTickBoundary() throws IOException {
        boolean found = false;
        for (Method handler : TileEntityMekanismMixin.class.getDeclaredMethods()) {
            Redirect redirect = handler.getAnnotation(Redirect.class);
            if (redirect == null || Arrays.stream(redirect.method()).noneMatch("tickServer"::equals)) {
                continue;
            }

            found = true;
            assertTrue(Modifier.isStatic(handler.getModifiers()));
            assertTrue(redirect.at().target().contains("TileEntityMekanism;onUpdateServer()V"));
        }
        assertTrue(found, "Mechanical scheduling must wrap tickServer's virtual onUpdateServer call");

        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/TileEntityMekanismMixin.java"));
        assertTrue(source.contains("WorkScheduler.advance("));
        assertTrue(source.contains("invoker.dimensionworks$onUpdateServer();"));
        assertTrue(source.contains("MachineWorkContext.push("));
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
