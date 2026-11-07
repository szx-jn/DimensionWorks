package dev.szx.dimensionworks.mekstress.integration.mek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ZeroEnergyContainerTest {

    @Test
    void neverStoresOrTransfersEnergy() {
        ZeroEnergyContainer container = new ZeroEnergyContainer();

        container.setEnergy(FloatingLong.create(100));
        container.deserializeNBT(new CompoundTag());

        assertEquals(FloatingLong.ZERO, container.getEnergy());
        assertEquals(FloatingLong.ZERO, container.getMaxEnergy());
        assertEquals(FloatingLong.ZERO, container.extract(FloatingLong.ONE, Action.EXECUTE, AutomationType.MANUAL));
        assertEquals(FloatingLong.ONE, container.insert(FloatingLong.ONE, Action.EXECUTE, AutomationType.MANUAL));
        assertTrue(container.isEmpty());
        assertTrue(container.serializeNBT().isEmpty());
    }
}
