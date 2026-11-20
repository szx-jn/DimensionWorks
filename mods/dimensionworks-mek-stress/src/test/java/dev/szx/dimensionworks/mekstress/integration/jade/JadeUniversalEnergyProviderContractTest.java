package dev.szx.dimensionworks.mekstress.integration.jade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JadeUniversalEnergyProviderContractTest {
    @Test
    void suppressesJadeUniversalEnergyForMekanismTiles() throws IOException {
        String mixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/JadeUniversalEnergyProviderMixin.java"));
        String clientMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/JadeUniversalEnergyProviderClientMixin.java"));
        String config = Files.readString(Path.of("src/main/resources/dimensionworks_mek_stress.mixins.json"));
        String visibility = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/integration/jade/JadeEnergyVisibility.java"));

        assertTrue(config.contains("mek.JadeUniversalEnergyProviderMixin"));
        assertTrue(config.contains("mek.JadeUniversalEnergyProviderClientMixin"));
        assertTrue(mixin.contains("EnergyStorageProvider"));
        assertTrue(mixin.contains("putData"));
        assertTrue(mixin.contains("JadeEnergyVisibility.shouldHide"));
        assertTrue(clientMixin.contains("appendTooltip"));
        assertTrue(clientMixin.contains("JadeEnergyVisibility.shouldHide"));
        assertTrue(visibility.contains("TileEntityBoundingBlock"));
        assertTrue(visibility.contains("getMainTile(bounding.getMainPos())"));
    }
}
