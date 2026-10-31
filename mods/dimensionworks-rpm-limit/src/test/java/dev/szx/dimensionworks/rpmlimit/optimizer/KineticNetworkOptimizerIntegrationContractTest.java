package dev.szx.dimensionworks.rpmlimit.optimizer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KineticNetworkOptimizerIntegrationContractTest {

    @Test
    void playerRpmLimitChangesInvalidateAndScheduleNetworkRecalculation() throws IOException {
        String manager = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitManager.java"));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(manager.contains("KineticNetworkOptimizer.invalidateAndRefreshFor(k)"));
        assertTrue(optimizer.contains("public static void invalidateAndRefreshFor("));
        assertTrue(optimizer.contains("candidate.networkDirty = true"));
    }

    @Test
    void rpmAndCreateConfigurationReloadsInvalidateCachedContributions() throws IOException {
        String mod = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/DimensionWorksRpmLimit.java"));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(mod.contains("KineticNetworkOptimizer.invalidateAllLoadedNetworks()"));
        assertTrue(optimizer.contains("public static void invalidateAllLoadedNetworks()"));
    }

    @Test
    void lifecycleInvalidationDoesNotForceAChunkUnloadRefresh() throws IOException {
        String lifecycle = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/minecraft/KineticBlockEntityLifecycleMixin.java"));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(lifecycle.contains("KineticNetworkOptimizer.invalidateFor(kinetic)"));
        assertTrue(optimizer.contains("Invalidates totals without forcing work during block removal or chunk unload."));
    }

    @Test
    void silentlyLoadedMembersScheduleOneNetworkSyncEvenForUnchangedTotals() throws IOException {
        String mixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixin.java"));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(mixin.contains("KineticPersistence.restore("));
        assertTrue(mixin.contains("@Redirect("));
        assertTrue(mixin.contains("KineticNetworkOptimizer.syncDuringNetworkRefresh(network)"));
        assertTrue(mixin.contains("KineticNetworkOptimizer.finishLoadedNetworkRefresh("));
        assertTrue(optimizer.contains("member.networkDirty = true"));
        assertTrue(optimizer.contains("if (alreadySynced != null && !alreadySynced)"));
    }

    @Test
    void staleMembersFailTheNetworkLimitCacheWhenStressPruningRemovesThem() throws IOException {
        String mixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixin.java"));
        String virtualMixin = Files.readString(Path.of(
            "../dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/mixin/create/KineticNetworkMixin.java"));
        String optimizer = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java"));

        assertTrue(virtualMixin.contains("KineticNetworkOptimizer.onMemberPruned("));
        assertTrue(mixin.contains("finishCapacityRebuild(self, naturalCapacity)"));
        assertTrue(optimizer.contains("if (sourceSetChanged) NetworkRpmLimit.invalidate(network)"));
    }

    @Test
    void createUpdateMethodsKeepTheirNormalBodiesOutsideTheBatch() throws IOException {
        String mixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixin.java"));

        assertTrue(mixin.contains("method = \"updateCapacityFor\""));
        assertTrue(mixin.contains("method = \"updateStressFor\""));
        assertTrue(mixin.contains("value = \"INVOKE\""));
        assertTrue(mixin.contains("dimensionworks$batchCapacityRefresh"));
        assertTrue(mixin.contains("dimensionworks$batchStressRefresh"));
    }
}
