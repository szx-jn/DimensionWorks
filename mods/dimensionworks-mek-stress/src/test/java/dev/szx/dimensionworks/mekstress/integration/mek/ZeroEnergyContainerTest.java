package dev.szx.dimensionworks.mekstress.integration.mek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ZeroEnergyContainerTest {

    @Test
    void neverStoresOrTransfersEnergy() {
        ZeroEnergyContainer container = new ZeroEnergyContainer();

        container.setEnergy(100L);
        container.deserializeNBT(net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty()), new CompoundTag());

        assertEquals(0L, container.getEnergy());
        assertEquals(0L, container.getMaxEnergy());
        assertEquals(0L, container.extract(1L, Action.EXECUTE, AutomationType.MANUAL));
        assertEquals(1L, container.insert(1L, Action.EXECUTE, AutomationType.MANUAL));
        assertTrue(container.isEmpty());
        assertTrue(container.serializeNBT(net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty())).isEmpty());
    }
}
