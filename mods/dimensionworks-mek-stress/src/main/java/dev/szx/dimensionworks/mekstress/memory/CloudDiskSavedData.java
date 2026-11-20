package dev.szx.dimensionworks.mekstress.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent, owner-isolated recovery stock; only explicit menu withdrawals mutate entries. */
public final class CloudDiskSavedData extends SavedData {
    private final Map<UUID, List<ItemStack>> stock = new HashMap<>();
    private final Map<UUID, Integer> notices = new HashMap<>();
    private long revision;

    public long revision() { return revision; }

    public static CloudDiskSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CloudDiskSavedData::new, CloudDiskSavedData::load, null), "dw_cloud_disk");
    }

    public void deposit(UUID owner, List<ItemStack> entries) {
        List<ItemStack> copies = entries.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        if (copies.isEmpty()) return;
        List<ItemStack> target = stock.computeIfAbsent(owner, ignored -> new ArrayList<>());
        for (ItemStack stack : copies) {
            while (!stack.isEmpty()) target.add(stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
        }
        notices.merge(owner, 1, Integer::sum);
        revision++;
        setDirty();
    }

    public int size(UUID owner) { return stock.getOrDefault(owner, List.of()).size(); }

    public ItemStack view(UUID owner, int index) {
        List<ItemStack> entries = stock.get(owner);
        return entries == null || index < 0 || index >= entries.size() ? ItemStack.EMPTY : entries.get(index).copy();
    }

    /** The insertion callback mutates a copy; an inventory-full remainder stays in the cloud. */
    public boolean claim(UUID owner, int index, Consumer<ItemStack> insert) {
        List<ItemStack> entries = stock.get(owner);
        if (entries == null || index < 0 || index >= entries.size()) return false;
        ItemStack original = entries.get(index);
        ItemStack remainder = original.copy();
        insert.accept(remainder);
        if (remainder.getCount() == original.getCount()) return false;
        if (remainder.isEmpty()) entries.remove(index);
        else entries.set(index, remainder);
        if (entries.isEmpty()) stock.remove(owner);
        revision++;
        setDirty();
        return true;
    }

    public int consumeNotices(UUID owner) {
        Integer count = notices.remove(owner);
        if (count != null) setDirty();
        return count == null ? 0 : count;
    }

    public static CloudDiskSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        CloudDiskSavedData data = new CloudDiskSavedData();
        if (root.contains("Owners") && !root.contains("Owners", Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Invalid cloud disk owner records");
        CompoundTag owners = root.getCompound("Owners");
        for (String key : owners.getAllKeys()) {
            UUID owner = UUID.fromString(key);
            if (!owners.contains(key, Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Invalid cloud disk owner record: " + key);
            CompoundTag record = owners.getCompound(key);
            if (record.contains("Items") && !record.contains("Items", Tag.TAG_LIST))
                throw new IllegalArgumentException("Invalid cloud disk item list: " + key);
            List<ItemStack> entries = new ArrayList<>();
            ListTag items = SavedInventoryNbt.compoundList(record, "Items");
            if (!items.isEmpty() && items.getElementType() != Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Invalid cloud disk item entries: " + key);
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = ItemStack.parse(registries, items.getCompound(i))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid cloud disk item stack: " + key));
                if (stack.isEmpty()) throw new IllegalArgumentException("Empty cloud disk item stack: " + key);
                entries.add(stack);
            }
            if (!entries.isEmpty()) data.stock.put(owner, entries);
            int notices = record.getInt("Notices");
            if (notices < 0) throw new IllegalArgumentException("Invalid cloud disk notice count: " + key);
            if (notices > 0) data.notices.put(owner, notices);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        CompoundTag owners = new CompoundTag();
        java.util.Set<UUID> ids = new java.util.HashSet<>(stock.keySet());
        ids.addAll(notices.keySet());
        for (UUID owner : ids) {
            CompoundTag record = new CompoundTag();
            ListTag items = new ListTag();
            stock.getOrDefault(owner, List.of()).forEach(stack -> items.add(stack.save(registries)));
            record.put("Items", items);
            record.putInt("Notices", notices.getOrDefault(owner, 0));
            owners.put(owner.toString(), record);
        }
        root.put("Owners", owners);
        return root;
    }
}
