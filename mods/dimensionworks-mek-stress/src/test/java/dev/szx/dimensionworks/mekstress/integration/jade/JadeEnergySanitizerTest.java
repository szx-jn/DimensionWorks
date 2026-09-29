package dev.szx.dimensionworks.mekstress.integration.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class JadeEnergySanitizerTest {
    @Test
    void removesEnergyEntryForEligibleMachine() {
        CompoundTag data = dataWithTextAndEnergy();

        assertTrue(JadeEnergySanitizer.removeEnergyElements(data, true));
        ListTag remaining = data.getList("mekData", CompoundTag.TAG_COMPOUND);
        assertEquals(1, remaining.size());
        assertTrue(remaining.getCompound(0).contains("text"));
        assertFalse(remaining.getCompound(0).contains("energy"));
    }

    @Test
    void leavesEnergyEntryForIneligibleMachine() {
        CompoundTag data = dataWithTextAndEnergy();

        assertFalse(JadeEnergySanitizer.removeEnergyElements(data, false));
        assertEquals(2, data.getList("mekData", CompoundTag.TAG_COMPOUND).size());
    }

    private static CompoundTag dataWithTextAndEnergy() {
        CompoundTag text = new CompoundTag();
        text.putString("text", "{\"text\":\"Machine\"}");
        CompoundTag energy = new CompoundTag();
        energy.putString("energy", "100");
        energy.putString("max", "1000");
        ListTag list = new ListTag();
        list.add(text);
        list.add(energy);
        CompoundTag data = new CompoundTag();
        data.put("mekData", list);
        return data;
    }
}
