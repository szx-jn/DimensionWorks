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

    public static TerritorySavedData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(TerritorySavedData::load, TerritorySavedData::new, DATA_NAME);
    }

    public static TerritorySavedData load(CompoundTag tag) {
        TerritorySavedData data = new TerritorySavedData();
        data.nextSpiralIndex = tag.getInt("NextSpiralIndex");

        ListTag list = tag.getList("Territories", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID id = parseUuid(entry.getString("Player"));
            if (id == null) {
                continue;
            }
            Territory territory = new Territory(
                    entry.getInt("SpiralIndex"), entry.getInt("GridX"), entry.getInt("GridZ"));
            data.territories.put(id, territory);
            if (entry.getBoolean("Completed")) {
                data.completed.add(id);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextSpiralIndex", nextSpiralIndex);

        ListTag list = new ListTag();
        territories.forEach((id, territory) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Player", id.toString());
            entry.putInt("SpiralIndex", territory.spiralIndex());
            entry.putInt("GridX", territory.gridX());
            entry.putInt("GridZ", territory.gridZ());
            entry.putBoolean("Completed", completed.contains(id));
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

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
