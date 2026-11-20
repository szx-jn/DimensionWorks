package dev.szx.dimensionworks.transfer.territory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persists plot assignments so a player always gets the same plot back. */
public class TerritorySavedData extends SavedData {
    public static final String DATA_NAME = "dimensionworks_transfer_territories";

    private int nextSpiralIndex;
    private final Map<UUID, Territory> territories = new HashMap<>();
    private final Set<UUID> completed = new HashSet<>();
    private final Set<UUID> alexCavesUnlocked = new HashSet<>();

    public static TerritorySavedData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TerritorySavedData::new, (tag, provider) -> load(tag)), DATA_NAME);
    }

    public static TerritorySavedData load(CompoundTag tag) {
        TerritorySavedData data = new TerritorySavedData();
        if (!tag.contains("NextSpiralIndex", Tag.TAG_INT) || tag.getInt("NextSpiralIndex") < 0) {
            throw new IllegalArgumentException("Invalid saved territory allocation index");
        }
        data.nextSpiralIndex = tag.getInt("NextSpiralIndex");
        if (!tag.contains("Territories", Tag.TAG_LIST) || !(tag.get("Territories") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid saved territory list");
        }
        Set<Integer> allocated = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID id = UUID.fromString(entry.getString("Player"));
            for (String key : new String[]{"SpiralIndex", "GridX", "GridZ"}) {
                if (!entry.contains(key, Tag.TAG_INT)) throw new IllegalArgumentException("Invalid saved territory coordinate: " + key);
            }
            for (String key : new String[]{"Completed", "AlexCavesUnlocked"}) {
                if (entry.contains(key) && !entry.contains(key, Tag.TAG_BYTE)) throw new IllegalArgumentException("Invalid saved territory flag: " + key);
            }
            Territory territory = new Territory(entry.getInt("SpiralIndex"), entry.getInt("GridX"), entry.getInt("GridZ"));
            int index = territory.spiralIndex();
            if (index < 0 || index >= data.nextSpiralIndex || !allocated.add(index)) throw new IllegalArgumentException("Invalid or duplicate saved territory allocation");
            int[] expected = SpiralAllocator.indexToGrid(index);
            if (expected[0] != territory.gridX() || expected[1] != territory.gridZ()) throw new IllegalArgumentException("Saved territory grid differs from its allocation");
            if (data.territories.putIfAbsent(id, territory) != null) throw new IllegalArgumentException("Duplicate saved territory owner: " + id);
            if (entry.getBoolean("Completed")) data.completed.add(id);
            if (entry.getBoolean("AlexCavesUnlocked")) data.alexCavesUnlocked.add(id);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        tag.putInt("NextSpiralIndex", nextSpiralIndex);

        ListTag list = new ListTag();
        territories.forEach((id, territory) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Player", id.toString());
            entry.putInt("SpiralIndex", territory.spiralIndex());
            entry.putInt("GridX", territory.gridX());
            entry.putInt("GridZ", territory.gridZ());
            entry.putBoolean("Completed", completed.contains(id));
            entry.putBoolean("AlexCavesUnlocked", alexCavesUnlocked.contains(id));
            list.add(entry);
        });
        tag.put("Territories", list);
        return tag;
    }

    /** Returns the caller's plot, claiming the next free spiral cell on first use. */
    public Territory getOrAssign(UUID player) {
        Territory existing = territories.get(player);
        if (existing != null) {
            return existing;
        }

        int index = nextSpiralIndex++;
        int[] grid = SpiralAllocator.indexToGrid(index);
        Territory created = new Territory(index, grid[0], grid[1]);
        territories.put(player, created);
        setDirty();
        return created;
    }

    public Territory get(UUID player) {
        return territories.get(player);
    }

    public boolean isComplete(UUID player) {
        return completed.contains(player);
    }

    public void markComplete(UUID player) {
        if (completed.add(player)) {
            setDirty();
        }
    }

    public boolean alexCavesUnlocked(UUID player) {
        return alexCavesUnlocked.contains(player);
    }

    public void unlockAlexCaves(UUID player) {
        if (alexCavesUnlocked.add(player)) setDirty();
    }

}
