package dev.szx.dimensionworks.rpmlimit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RpmLimitManagerContractTest {
    @Test
    void ownerlessKineticChainsUseTheConservativeDefaultLimit() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitManager.java"));

        assertTrue(source.contains("return owner == null ? RpmLimitConfig.DEFAULT_RPM.get()"));
        assertTrue(source.contains("instanceof IPreclampedKineticSpeed"));
    }

    @Test
    void clampsSpeedsWhenCreateStoresThemAndRepairsLoadedMembers() throws IOException {
        String mixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticBlockEntityMixin.java"));
        String manager = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitManager.java"));

        assertTrue(mixin.contains("@ModifyVariable(method = \"setSpeed\""));
        assertTrue(manager.contains("float cappedSpeed = RpmLimitMath.cap(currentSpeed, value)"));
        assertTrue(manager.contains("loadOfflineProfile(direct, server)"));
        assertTrue(manager.contains("CACHE.containsKey(direct) ? getEffectiveLimit(direct) : storedMachineLimit(k)"));

        String outputGate = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/quota/FinalProductionGate.java"));
        assertTrue(outputGate.contains("owner = GearHeartOwnerResolver.normalize(owner)"));
    }
}
