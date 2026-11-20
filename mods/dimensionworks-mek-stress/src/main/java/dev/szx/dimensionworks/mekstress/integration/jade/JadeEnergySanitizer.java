package dev.szx.dimensionworks.mekstress.integration.jade;

import mekanism.api.SerializationConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Removes Mekanism's FE/J Jade element from every Mekanism tile. */
public final class JadeEnergySanitizer {
    private JadeEnergySanitizer() {
    }

    public static boolean removeEnergyElements(CompoundTag data) {
        if (data == null || !data.contains("mek_data", Tag.TAG_LIST)) {
            return false;
        }
        ListTag entries = data.getList("mek_data", Tag.TAG_COMPOUND);
        boolean removed = false;
        for (int index = entries.size() - 1; index >= 0; index--) {
            CompoundTag entry = entries.getCompound(index);
            if (entry.contains(SerializationConstants.ENERGY)) {
                entries.remove(index);
                removed = true;
            }
        }
        if (entries.isEmpty()) {
            data.remove("mek_data");
        }
        return removed;
    }
}
