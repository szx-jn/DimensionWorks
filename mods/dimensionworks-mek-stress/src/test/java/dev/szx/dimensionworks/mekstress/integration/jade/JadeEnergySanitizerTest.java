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

        assertTrue(JadeEnergySanitizer.removeEnergyElements(data));
        ListTag remaining = data.getList("mek_data", CompoundTag.TAG_COMPOUND);
        assertEquals(1, remaining.size());
        assertTrue(remaining.getCompound(0).contains("text"));
        assertFalse(remaining.getCompound(0).contains("energy"));
    }

    @Test
    void removesEnergyForEveryMekanismMachine() {
        CompoundTag data = dataWithTextAndEnergy();

        assertTrue(JadeEnergySanitizer.removeEnergyElements(data));
        assertEquals(1, data.getList("mek_data", CompoundTag.TAG_COMPOUND).size());
    }

    @Test
    void removesEnergyEntryRegardlessOfStoredValueType() {
        CompoundTag energy = new CompoundTag();
        energy.putInt("energy", 100);
        energy.putInt("max", 1000);
        ListTag list = new ListTag();
        list.add(energy);
        CompoundTag data = new CompoundTag();
        data.put("mek_data", list);

        assertTrue(JadeEnergySanitizer.removeEnergyElements(data));
        assertFalse(data.contains("mek_data"));
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
        data.put("mek_data", list);
        return data;
    }
}
