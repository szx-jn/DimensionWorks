package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.core.ChannelDirectory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Global channel claim metadata. Contents and upgrades require separate persistent storage. */
public final class ChannelDirectorySavedData extends SavedData {
    private static final String NAME = "dimensionworks_logistics_channels";
    private ChannelDirectory directory = new ChannelDirectory();
    private final Map<ChannelDirectory.Endpoint, Integer> tiers = new HashMap<>();

    public static ChannelDirectorySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(ChannelDirectorySavedData::load, ChannelDirectorySavedData::new, NAME);
    }

    public ChannelDirectory.Channel getChannel(int number) { return directory.get(number); }

    public boolean join(int number, UUID player, ChannelDirectory.Endpoint endpoint,
                        BiPredicate<UUID, UUID> sameTeam) {
        return join(number, player, endpoint, 1, sameTeam);
    }

    public boolean join(int number, UUID player, ChannelDirectory.Endpoint endpoint, int tier,
                        BiPredicate<UUID, UUID> sameTeam) {
        if (tier < 1 || tier > 3) throw new IllegalArgumentException("Invalid endpoint tier");
        boolean joined = directory.join(number, player, endpoint, sameTeam);
        if (joined) {
            tiers.put(endpoint, tier);
            setDirty();
        }
        return joined;
    }

    public boolean removeEndpoint(ChannelDirectory.Endpoint endpoint) {
        boolean last = directory.removeEndpoint(endpoint);
        tiers.remove(endpoint);
        // Mark even when this was not the last endpoint; removal may still have changed the data.
        setDirty();
        return last;
    }

    public void releaseEmpty(int number) {
        directory.releaseEmpty(number);
        setDirty();
    }

    public int tierForChannel(int number) {
        ChannelDirectory.Channel channel = directory.get(number);
        if (channel == null) return 1;
        return channel.endpoints().stream().mapToInt(endpoint -> tiers.getOrDefault(endpoint, 1)).min().orElse(1);
    }

    public void refreshTier(ChannelDirectory.Endpoint endpoint, int tier) {
        if (tier < 1 || tier > 3) throw new IllegalArgumentException("Invalid endpoint tier");
        Integer number = directory.snapshot().entrySet().stream()
            .filter(entry -> entry.getValue().endpoints().contains(endpoint))
            .map(Map.Entry::getKey).findFirst().orElse(null);
        if (number != null && !Integer.valueOf(tier).equals(tiers.put(endpoint, tier))) setDirty();
    }

    public static ChannelDirectorySavedData load(CompoundTag tag) {
        ChannelDirectorySavedData result = new ChannelDirectorySavedData();
        Map<Integer, ChannelDirectory.Channel> saved = new HashMap<>();
        if (tag.contains("Channels") && !tag.contains("Channels", Tag.TAG_LIST))
            throw new IllegalArgumentException("Invalid saved channel list");
        ListTag channels = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < channels.size(); i++) {
            CompoundTag entry = channels.getCompound(i);
            if (!entry.contains("Endpoints", Tag.TAG_LIST))
                throw new IllegalArgumentException("Channel has no endpoint list");
            if (!entry.hasUUID("Owner")) throw new IllegalArgumentException("Channel has no owner");
            int number = entry.getInt("Number");
            Set<ChannelDirectory.Endpoint> endpoints = new HashSet<>();
            ListTag list = entry.getList("Endpoints", Tag.TAG_COMPOUND);
            for (int j = 0; j < list.size(); j++) {
                CompoundTag node = list.getCompound(j);
                if (!node.contains("Dimension", Tag.TAG_STRING) || !node.contains("Position", Tag.TAG_LONG)
                    || !node.contains("Tier", Tag.TAG_INT))
                    throw new IllegalArgumentException("Invalid saved endpoint");
                var endpoint = new ChannelDirectory.Endpoint(node.getString("Dimension"), node.getLong("Position"));
                if (!endpoints.add(endpoint)) throw new IllegalArgumentException("Duplicate endpoint");
                int tier = node.getInt("Tier");
                if (tier < 1 || tier > 3) throw new IllegalArgumentException("Invalid saved endpoint tier");
                result.tiers.put(endpoint, tier);
            }
            if (saved.putIfAbsent(number, new ChannelDirectory.Channel(entry.getUUID("Owner"), endpoints)) != null)
                throw new IllegalArgumentException("Duplicate channel");
        }
        result.directory = ChannelDirectory.restore(saved);
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag channels = new ListTag();
        directory.snapshot().forEach((number, channel) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Number", number);
            entry.putUUID("Owner", channel.owner());
            ListTag endpoints = new ListTag();
            for (var endpoint : channel.endpoints()) {
                CompoundTag node = new CompoundTag();
                node.putString("Dimension", endpoint.dimension());
                node.putLong("Position", endpoint.position());
                node.putInt("Tier", tiers.getOrDefault(endpoint, 1));
                endpoints.add(node);
            }
            entry.put("Endpoints", endpoints);
            channels.add(entry);
        });
        tag.put("Channels", channels);
        return tag;
    }
}
