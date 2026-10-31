package dev.szx.dimensionworks.rpmlimit.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KineticNetworkMixinContractTest {

    private static final String SOURCE_PATH =
        "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixin.java";

    @Test
    void calculateCapacityDelegatesToTheOwnerAggregate() throws IOException {
        String source = Files.readString(Path.of(SOURCE_PATH));

        assertEquals(2, occurrences(source, "@Inject(method = \"calculateCapacity\""));
        assertTrue(source.contains("CreateCapacityRegistry.limitNaturalCapacity("));
        assertTrue(source.contains("KineticNetworkOptimizer.cachedCapacity(self)"));
        assertTrue(source.contains("KineticNetworkOptimizer.finishCapacityRebuild(self, naturalCapacity)"));
        assertTrue(source.contains("CallbackInfoReturnable<Float>"));
        assertFalse(source.contains("stressCapacityMultiplier("));
        assertFalse(source.contains(".calculateCapacity())"));
    }

    @Test
    void overspeedStressIsAppliedAtTheActualStressBoundary() throws IOException {
        String network = Files.readString(Path.of(SOURCE_PATH));
        String blockEntity = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticBlockEntityMixin.java"));

        assertTrue(network.contains("OverspeedBonus.stressMultiplier(be)"));
        assertFalse(blockEntity.contains("dimensionworks$overspeedStress"));
    }

    @Test
    void sourcePruningCannotLeaveAStaleNetworkLimitCache() throws IOException {
        String source = Files.readString(Path.of(SOURCE_PATH));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(source.contains("finishCapacityRebuild(self, naturalCapacity)"));
        assertTrue(optimizer.contains("if (sourceSetChanged) NetworkRpmLimit.invalidate(network)"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
