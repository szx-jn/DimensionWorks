package dev.szx.dimensionworks.rpmlimit.mixin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FinalProductionMixinContractTest {

    @Test
    void createOutputBoundariesCallTheGate() throws IOException {
        assertSourceContains("BasinOperatingBlockEntityMixin.java",
            "FinalProductionGate.tryConsumeOutputs(");
        assertSourceContains("MillstoneBlockEntityMixin.java",
            "FinalProductionGate.tryConsumeOutputs(");
        assertSourceContains("SawBlockEntityMixin.java",
            "FinalProductionGate.tryConsumeOutputs(");
        assertSourceContains("MechanicalPressBlockEntityMixin.java",
            "FinalProductionGate.tryConsumeOutputs(");
    }

    @Test
    void createMixinsSimulateOutputSpaceBeforeConsuming() throws IOException {
        assertSourceContains("BasinOperatingBlockEntityMixin.java", "BasinRecipe.match(");
        assertSourceContains("MillstoneBlockEntityMixin.java", "insertItemStacked(");
        assertSourceContains("SawBlockEntityMixin.java", "dimensionworks$getRecipes(");
        assertSourceContains("MechanicalPressBlockEntityMixin.java", "getRecipe(");
    }

    @Test
    void mekOutputHelperUsesCurrentOwnerContext() throws IOException {
        String source = Files.readString(Path.of(
            "../dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/OutputHelperMixin.java"));

        assertTrue(source.contains("MekProcessingContext.current()"));
        assertTrue(source.contains("FinalProductionGate.tryConsume("));
        assertTrue(source.contains("AutomationType.INTERNAL"));
    }

    private static void assertSourceContains(String fileName, String needle) throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/" + fileName));
        assertTrue(source.contains(needle), fileName + " missing " + needle);
    }
}
