package dev.szx.dimensionworks.mekstress.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Removes Mekanism's FE/J Jade element for machines whose work is now SU-powered. */
public final class JadeEnergySanitizer {
    private JadeEnergySanitizer() {
    }

    public static boolean removeEnergyElements(CompoundTag data, boolean eligible) {
        if (!eligible || data == null || !data.contains("mekData", Tag.TAG_LIST)) {
            return false;
        }
        ListTag entries = data.getList("mekData", Tag.TAG_COMPOUND);
        boolean removed = false;
        for (int index = entries.size() - 1; index >= 0; index--) {
            CompoundTag entry = entries.getCompound(index);
            if (entry.contains("energy", Tag.TAG_STRING)) {
                entries.remove(index);
                removed = true;
            }
        }
        if (entries.isEmpty()) {
            data.remove("mekData");
        }
        return removed;
    }
}
