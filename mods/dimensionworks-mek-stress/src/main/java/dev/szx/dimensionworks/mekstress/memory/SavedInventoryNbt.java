package dev.szx.dimensionworks.mekstress.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Strict list reading for persisted inventories: getList would hide a wrong element type. */
public final class SavedInventoryNbt {
    private SavedInventoryNbt() {}

    public static ListTag compoundList(CompoundTag parent, String key) {
        Tag value = parent.get(key);
        if (value == null) return new ListTag();
        if (!(value instanceof ListTag list)
            || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Invalid saved inventory list: " + key);
        return list;
    }
}
