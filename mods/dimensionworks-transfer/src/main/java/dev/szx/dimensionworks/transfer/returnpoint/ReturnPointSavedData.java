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
                .computeIfAbsent(new SavedData.Factory<>(ReturnPointSavedData::new, (tag, provider) -> load(tag)), DATA_NAME);
    }

    public static ReturnPointSavedData load(CompoundTag tag) {
        ReturnPointSavedData data = new ReturnPointSavedData();
        if (!tag.contains("ReturnPoints", Tag.TAG_LIST) || !(tag.get("ReturnPoints") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid saved return-point list");
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID player = UUID.fromString(entry.getString("Player"));
            if (!entry.contains("ReturnPoint", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing saved return point");
            var point = ReturnPoint.load(entry.getCompound("ReturnPoint"));
            if (data.points.putIfAbsent(player, point) != null) throw new IllegalArgumentException("Duplicate saved return-point owner: " + player);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
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

}
