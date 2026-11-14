package dev.szx.dimensionworks.cavefactory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persisted outdoor exposure timers for molten-metal fluid blocks in one dimension. */
public final class MoltenCrystallizationData extends SavedData {
    private static final String DATA_NAME = "dimensionworks_molten_crystallization";

    // A negative timestamp means the fluid is currently covered or has not yet been observed.
    private final Map<Long, Long> exposureStarted = new HashMap<>();

    public static MoltenCrystallizationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            MoltenCrystallizationData::load,
            MoltenCrystallizationData::new,
            DATA_NAME
        );
    }

    private static MoltenCrystallizationData load(CompoundTag tag) {
        MoltenCrystallizationData data = new MoltenCrystallizationData();
        ListTag entries = tag.getList("positions", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            data.exposureStarted.put(entry.getLong("pos"), entry.getLong("started"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        exposureStarted.forEach((pos, started) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos);
            entry.putLong("started", started);
            entries.add(entry);
        });
        tag.put("positions", entries);
        return tag;
    }

    public Map<Long, Long> positions() {
        return exposureStarted;
    }

    public void reset(BlockPos pos) {
        exposureStarted.put(pos.asLong(), -1L);
        setDirty();
    }
}
