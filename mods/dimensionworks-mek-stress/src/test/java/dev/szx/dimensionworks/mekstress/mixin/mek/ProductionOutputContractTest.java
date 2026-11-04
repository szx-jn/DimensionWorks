package dev.szx.dimensionworks.mekstress.mixin.mek;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionOutputContractTest {
    @Test
    void scalesEveryBuiltInMekanismOutputAtTheRecipeBoundary() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/OutputHelperMixin.java"));
        String scaledHandler = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/integration/mek/ScaledOutputHandler.java"));
        String delivery = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/integration/mek/MachineOutputDelivery.java"));

        assertTrue(source.contains("handleOutput(Lmekanism/api/inventory/IInventorySlot;Lnet/minecraft/world/item/ItemStack;I)V"));
        assertTrue(source.contains("handleOutput(Lmekanism/api/chemical/IChemicalTank;Lmekanism/api/chemical/ChemicalStack;I)V"));
        assertTrue(source.contains("handleOutput(Lmekanism/api/fluid/IExtendedFluidTank;Lnet/minecraftforge/fluids/FluidStack;I)V"));
        assertTrue(source.contains("scaleOutput("));
        assertTrue(source.contains("wrapChanceOutputHandler"));
        assertTrue(source.contains("wrapReactionOutputHandler"));
        assertTrue(source.contains("wrapElectrolysisOutputHandler"));
        assertTrue(scaledHandler.contains("handleChanceOutput"));
        assertTrue(scaledHandler.contains("output.getMaxSecondaryOutput()"));
        assertTrue(scaledHandler.contains("MachineOutputDelivery.insertItemScaled(secondarySlot"));
        assertTrue(delivery.contains("remaining -= inserted"));
    }

    @Test
    void scaledOutputDeliveryCannotBypassFinalProductionQuota() throws IOException {
        String delivery = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/integration/mek/MachineOutputDelivery.java"));

        assertTrue(delivery.contains("FinalProductionGate.tryConsume("));
        assertTrue(delivery.contains("MekProcessingContext.current()"));
        assertTrue(delivery.contains("capItem"));
        assertTrue(delivery.contains("capChemical"));
        assertTrue(delivery.contains("capFluid"));
    }
}
