package dev.szx.dimensionworks.transfer.returnpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persists the location each player should return to after visiting the transfer dimension. */
public class ReturnPointSavedData extends SavedData {
    public static final String DATA_NAME = "dimensionworks_transfer_returns";

    private final Map<UUID, ReturnPoint> points = new HashMap<>();

    public static ReturnPointSavedData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(ReturnPointSavedData::load, ReturnPointSavedData::new, DATA_NAME);
    }

    public static ReturnPointSavedData load(CompoundTag tag) {
        ReturnPointSavedData data = new ReturnPointSavedData();
        ListTag list = tag.getList("ReturnPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID player = parseUuid(entry.getString("Player"));
            if (player == null || !entry.contains("ReturnPoint", Tag.TAG_COMPOUND)) {
                continue;
            }
            try {
                data.points.put(player, ReturnPoint.load(entry.getCompound("ReturnPoint")));
            } catch (IllegalArgumentException exception) {
                // Ignore corrupt entries instead of preventing the whole world from loading.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        points.forEach((player, point) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Player", player.toString());
            entry.put("ReturnPoint", point.save());
            list.add(entry);
        });
        tag.put("ReturnPoints", list);
        return tag;
    }

    public Optional<ReturnPoint> get(UUID player) {
        return Optional.ofNullable(points.get(player));
    }

    public void put(UUID player, ReturnPoint point) {
        points.put(player, point);
        setDirty();
    }

    public void remove(UUID player) {
        if (points.remove(player) != null) {
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
