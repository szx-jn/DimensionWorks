package dev.szx.dimensionworks.transfer.tutorial;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Keeps the original per-player tutorial state and full saved hand payload. */
public final class TutorialSavedData extends SavedData {
    public static final String DATA_NAME = "dw_dispatch_tutorial";
    private final Map<UUID, CompoundTag> states = new HashMap<>();

    public static TutorialSavedData get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(TutorialSavedData::new, (tag, provider) -> load(tag)), DATA_NAME);
    }

    public CompoundTag get(UUID player) { return states.get(player); }

    public void put(UUID player, CompoundTag state) {
        states.put(player, state.copy());
        setDirty();
    }

    public static TutorialSavedData load(CompoundTag root) {
        var data = new TutorialSavedData();
        for (String key : root.getAllKeys()) {
            if (!root.contains(key, Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("Invalid tutorial state: " + key);
            }
            data.states.put(UUID.fromString(key), root.getCompound(key).copy());
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        states.forEach((player, state) -> root.put(player.toString(), state.copy()));
        return root;
    }
}
