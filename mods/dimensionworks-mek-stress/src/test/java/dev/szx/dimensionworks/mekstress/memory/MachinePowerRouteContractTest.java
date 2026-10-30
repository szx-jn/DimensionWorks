package dev.szx.dimensionworks.mekstress.memory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MachinePowerRouteContractTest {
    @Test
    void adjacentKineticMemberJoinsItsExistingNetworkWithoutAnExportGearbox() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachinePowerManager.java"));

        assertTrue(source.contains("MachineFootprint.kineticCandidates("));
        assertTrue(source.contains("AttributeHasBounding"));
        assertTrue(source.contains("bounding.getPositions("));
        assertTrue(source.contains("KineticNetwork network = kinetic.getOrCreateNetwork();"));
        assertTrue(source.contains("if (network == null)"));
        assertTrue(source.contains("new DirectSource(kinetic, network,"));
        assertTrue(source.contains("PowerRouteSafety.shouldAssignStress("));
        assertTrue(source.contains("MachineKineticRegistry.sync(tile, route.network(), memberRpm(route));"));
        assertTrue(source.contains("MachineKineticRegistry.detach(tile);"));
        assertFalse(source.contains("findExportGearbox"));
    }

    @Test
    void routesOnlyThroughShaftsThatFaceTheMachine() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachinePowerManager.java"));

        assertTrue(source.contains("hasShaftTowards("));
        assertTrue(source.contains("getOpposite()"));
    }

    @Test
    void directRoutesRespectPlayerRpmLimitBeforeMekTierScaling() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachinePowerManager.java"));

        assertTrue(source.contains("RpmLimitManager.clamp(kinetic, kinetic.getTheoreticalSpeed())"));
        assertTrue(source.contains("MachinePowerMath.directRpm(limitedRpm"));
        assertTrue(source.contains("RpmLimitManager.clamp(gearbox, configuredRpm)"));
    }

    @Test
    void virtualMechanicalEnergyIsDynamicAndNeverWrittenIntoContainerStorage() throws IOException {
        String energyMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/MachineEnergyContainerMixin.java"));
        String manager = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachinePowerManager.java"));
        String tileMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/TileEntityMekanismMixin.java"));

        assertTrue(energyMixin.contains("MachinePowerManager.hasMechanicalPower(tile)"));
        assertTrue(energyMixin.contains("method = \"serializeNBT\""));
        assertTrue(energyMixin.contains("cir.setReturnValue(new CompoundTag())"));
        assertFalse(manager.contains("updateMechanicalEnergy"));
        assertFalse(manager.contains("setEnergy(target)"));
        assertFalse(tileMixin.contains("updateMechanicalEnergy"));
    }

    @Test
    void scopedNullRouteDoesNotTriggerRepeatedNetworkSearches() throws IOException {
        String scope = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/core/MachinePowerRouteScope.java"));
        String manager = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachinePowerManager.java"));

        assertTrue(scope.contains("boolean contains(TileEntityMekanism tile)"));
        assertTrue(manager.contains("if (MachinePowerRouteScope.contains(tile))"));
    }

    @Test
    void jadeReportsTheCombinedThroughputMultiplier() throws IOException {
        String jade = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/integration/jade/MemoryJadePlugin.java"));

        assertTrue(jade.contains("MachinePowerManager.throughputMultiplier(machine, route)"));
    }

    @Test
    void virtualMemberParticipatesInCreateNetworkStress() throws IOException {
        String registry = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachineKineticRegistry.java"));
        String member = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MachineKineticMember.java"));
        String networkMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/create/KineticNetworkMixin.java"));

        assertTrue(registry.contains("network.members.put("));
        assertTrue(registry.contains("flushDirtyNetworks"));
        assertTrue(registry.contains("network.updateNetwork()"));
        assertTrue(registry.contains("boolean wasAttached = network.members.containsKey(member);"));
        assertTrue(registry.contains("boolean topologyChanged = previous != network || !wasAttached;"));
        assertTrue(registry.contains("if (topologyChanged)"));
        assertTrue(member.contains("extends KineticBlockEntity"));
        assertTrue(member.contains("IVirtualKineticMember"));
        assertTrue(member.contains("IPreclampedKineticSpeed"));
        assertTrue(member.contains("calculateStressApplied()"));
        assertTrue(networkMixin.contains("IVirtualKineticMember"));
        assertTrue(networkMixin.contains("calculateStress"));
    }

    @Test
    void obsoleteSyntheticSourceStressIsRemoved() {
        assertFalse(Files.exists(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/DirectStressRegistry.java")));
    }
}
