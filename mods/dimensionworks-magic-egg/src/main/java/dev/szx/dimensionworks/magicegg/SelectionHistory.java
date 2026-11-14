package dev.szx.dimensionworks.magicegg;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class SelectionHistory {
    private static final String KEY = "dw_magic_century_egg_last_selection";

    private SelectionHistory() {}

    public static boolean exists(CompoundTag data) {
        return data.getCompound(Player.PERSISTED_NBT_TAG).contains(KEY, Tag.TAG_LIST);
    }

    public static List<String> read(CompoundTag data) {
        return data.getCompound(Player.PERSISTED_NBT_TAG).getList(KEY, Tag.TAG_STRING)
                .stream().map(Tag::getAsString).toList();
    }

    public static void save(CompoundTag data, List<String> effects) {
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = new ListTag();
        effects.forEach(id -> list.add(StringTag.valueOf(id)));
        persisted.put(KEY, list);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void copy(CompoundTag original, CompoundTag replacement) {
        if (exists(original)) save(replacement, read(original));
    }
}
