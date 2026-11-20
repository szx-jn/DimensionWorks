package dev.szx.dimensionworks.migration;

import com.wintercogs.ae2omnicells.common.me.AEUniversalCellData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.LegacyStorageBlockDataMigration;

import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** 隔离内存存储/效果读写；不放置真实方块、不生成生物、不读生产世界。 */
@Mod("dimensionworks_migration_storage_batch")
public final class NativeStorageBatch {
    public NativeStorageBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated fixture");
            Path output = cwd.resolve("local/migration-storage-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse existing native output");
            CompoundTag source = NbtIo.readCompressed(cwd.resolve("local/migration-storage-input.nbt"), NbtAccounter.unlimitedHeap());
            if (source.getInt("format") != 1) throw new IllegalStateException("Wrong storage batch format");
            var registry = event.getServer().registryAccess();
            var ops = RegistryOps.create(NbtOps.INSTANCE, registry);
            CompoundTag result = new CompoundTag(); result.putInt("format", 1);

            if (source.contains("backpacks", Tag.TAG_COMPOUND)) {
                CompoundTag old = source.getCompound("backpacks");
                BackpackStorage storage = BackpackStorage.load(old.copy(), registry);
                CompoundTag saved = storage.save(new CompoundTag(), registry);
                BackpackStorage second = BackpackStorage.load(saved.copy(), registry);
                ListTag details = new ListTag(); Set<UUID> uuids = new HashSet<>();
                for (Tag value : old.getList("backpackContents", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) value;
                    UUID uuid = row.getUUID("uuid");
                    if (!uuids.add(uuid)) throw new IllegalStateException("Duplicate backpack UUID");
                    CompoundTag contents = storage.getBackpackContents(uuid).orElseThrow(() -> new IllegalStateException("Native backpack dropped UUID"));
                    if (!contents.equals(second.getBackpackContents(uuid).orElseThrow()))
                        throw new IllegalStateException("Native backpack did not stabilize on reload");
                    CompoundTag detail = new CompoundTag(); detail.putUUID("uuid", uuid);
                    if (contents.contains("inventory") || contents.contains("upgradeInventory")) {
                        CompoundTag stack = new CompoundTag(), components = new CompoundTag();
                        stack.putString("id", "sophisticatedbackpacks:backpack"); stack.putInt("count", 1);
                        components.putInt("sophisticatedcore:number_of_inventory_slots", contents.getCompound("inventory").getInt("Size"));
                        components.putInt("sophisticatedcore:number_of_upgrade_slots", contents.getCompound("upgradeInventory").getInt("Size"));
                        components.putUUID("sophisticatedcore:storage_uuid", uuid); stack.put("components", components);
                        BackpackWrapper wrapper = suppliedBackpack(CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, stack).getOrThrow(), uuid, contents.copy());
                        if (contents.contains("inventory")) detail.put("inventory", snapshot(wrapper.getInventoryHandler(), ops));
                        if (contents.contains("upgradeInventory")) detail.put("upgradeInventory", snapshot(wrapper.getUpgradeHandler(), ops));
                    }
                    details.add(detail);
                }
                if (saved.getList("backpackContents", Tag.TAG_COMPOUND).size() != uuids.size())
                    throw new IllegalStateException("Native backpack changed complete UUID set");
                result.put("backpacks", saved); result.put("backpack_details", details);
            }

            ListTag rows = new ListTag();
            for (Tag value : source.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value, converted = new CompoundTag();
                converted.putString("label", row.getString("label")); converted.putString("kind", row.getString("kind"));
                try {
                    if (row.getString("kind").equals("storage_block_entity")) {
                        CompoundTag data = row.getCompound("data").copy();
                        BlockState state = BlockState.CODEC.parse(ops, row.getCompound("state")).getOrThrow();
                        BlockPos pos = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                        LegacyStorageBlockDataMigration.normalizeBlockEntityTag(data);
                        StorageBlockEntity tile = storageTile(state, pos); tile.setLevel(event.getServer().overworld());
                        tile.loadWithComponents(data, registry);
                        converted.put("inventory", snapshot(tile.getStorageWrapper().getInventoryHandler(), ops));
                        converted.put("upgradeInventory", snapshot(tile.getStorageWrapper().getUpgradeHandler(), ops));
                        CompoundTag saved = tile.saveWithFullMetadata(registry);
                        StorageBlockEntity second = storageTile(state, pos); second.setLevel(event.getServer().overworld());
                        second.loadWithComponents(saved.copy(), registry);
                        if (!saved.equals(second.saveWithFullMetadata(registry))) throw new IllegalStateException("Storage BE did not stabilize on reload");
                        converted.put("data", saved); converted.putBoolean("reload_stable", true);
                    } else if (row.getString("kind").equals("omni_saved_data")) {
                        AEUniversalCellData storage = AEUniversalCellData.FACTORY.deserializer().apply(row.getCompound("data").copy(), registry);
                        CompoundTag saved = storage.save(new CompoundTag(), registry);
                        if (!saved.getCompound("inventory").getList("error_entries", Tag.TAG_COMPOUND).isEmpty())
                            throw new IllegalStateException("Native universal cell contains pending key errors");
                        AEUniversalCellData second = AEUniversalCellData.FACTORY.deserializer().apply(saved.copy(), registry);
                        CompoundTag reloaded = second.save(new CompoundTag(), registry);
                        // 作者使用散列表保存 entries；列表顺序可以改变，完整键状态与 Long 数量不能变。
                        CompoundTag oldMeta = saved.copy(), newMeta = reloaded.copy();
                        oldMeta.getCompound("inventory").remove("entries"); newMeta.getCompound("inventory").remove("entries");
                        Map<String, Long> firstKeys = inventoryKeys(saved), secondKeys = inventoryKeys(reloaded);
                        if (!oldMeta.equals(newMeta) || !firstKeys.equals(secondKeys) ||
                            firstKeys.size() != storage.getOriginalStorage().size() || secondKeys.size() != second.getOriginalStorage().size())
                            throw new IllegalStateException("Universal cell keys, quantities or metadata changed on reload");
                        converted.put("reloaded_data", reloaded);
                        converted.put("data", saved); converted.putInt("types", storage.getOriginalStorage().size()); converted.putBoolean("reload_stable", true);
                    } else if (row.getString("kind").equals("active_effect")) {
                        CompoundTag data = row.getCompound("data");
                        MobEffectInstance effect = MobEffectInstance.CODEC.parse(ops, data).getOrThrow();
                        if (!effect.getEffect().unwrapKey().orElseThrow().location().toString().equals(data.getString("id")) ||
                            effect.getDuration() != data.getInt("duration") || effect.getAmplifier() != data.getInt("amplifier") ||
                            effect.isAmbient() != data.getBoolean("ambient") || effect.isVisible() != data.getBoolean("show_particles") ||
                            effect.showIcon() != data.getBoolean("show_icon") ||
                            !effect.getCures().equals(Set.of(EffectCures.MILK, EffectCures.PROTECTED_BY_TOTEM)))
                            throw new IllegalStateException("Native effect changed state or cures");
                        Tag saved = MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow();
                        MobEffectInstance second = MobEffectInstance.CODEC.parse(ops, saved).getOrThrow();
                        if (!saved.equals(MobEffectInstance.CODEC.encodeStart(ops, second).getOrThrow()))
                            throw new IllegalStateException("Native effect did not stabilize on reload");
                        converted.put("data", saved); converted.putBoolean("reload_stable", true);
                    } else if (row.getString("kind").equals("item")) {
                        ItemStack first = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, row.getCompound("stack")).getOrThrow();
                        Tag saved = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, first).getOrThrow();
                        ItemStack second = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, saved).getOrThrow();
                        if (first.isEmpty() || first.getItem() != second.getItem() || first.getCount() != second.getCount() ||
                            !saved.equals(CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, second).getOrThrow()) ||
                            !DataComponentMap.CODEC.encodeStart(ops, first.getComponents()).getOrThrow().equals(
                                DataComponentMap.CODEC.encodeStart(ops, second.getComponents()).getOrThrow()))
                            throw new IllegalStateException("Native item or effective components changed on reload");
                        converted.put("stack", saved); converted.putBoolean("reload_stable", true);
                    } else throw new IllegalStateException("Unknown storage row kind");
                    rejectMissing(converted); rows.add(converted);
                } catch (Exception error) { throw new IllegalStateException("Native storage failed at " + row.getString("label"), error); }
            }
            result.put("rows", rows); rejectMissing(result);
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(result, output);
            System.out.println("MIGRATION_STORAGE_BATCH_DONE rows=" + rows.size() + " backpacks=" + result.getList("backpack_details", Tag.TAG_COMPOUND).size());
        } catch (Exception error) { throw new RuntimeException("Native storage serialization batch failed", error); }
    }

    private static StorageBlockEntity storageTile(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof EntityBlock block) || !(block.newBlockEntity(pos, state) instanceof StorageBlockEntity tile))
            throw new IllegalStateException("Actual state does not construct Sophisticated storage");
        return tile;
    }

    private static BackpackWrapper suppliedBackpack(ItemStack stack, UUID uuid, CompoundTag contents) throws Exception {
        // 作者的包内接口允许提供独立 contents；避免将真实 UUID 装入夹具全局世界存储。
        Class<?> sourceType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackContentsSource");
        AtomicReference<CompoundTag> data = new AtomicReference<>(contents);
        Object source = Proxy.newProxyInstance(sourceType.getClassLoader(), new Class<?>[]{sourceType}, (proxy, method, args) -> switch (method.getName()) {
            case "getContents" -> data.get();
            case "getContentsIfPresent" -> Optional.of(data.get());
            case "getContentsUuid" -> Optional.of(uuid);
            case "setContents" -> { data.set((CompoundTag) args[0]); yield null; }
            case "markDirty" -> null;
            case "usesLegacyBackpackDataMigration" -> false;
            case "usesCanonicalSlotNumbers" -> true;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "IsolatedBackpackContents";
            default -> throw new IllegalStateException("Uncovered author contents-source method: " + method.getName());
        });
        var constructor = BackpackWrapper.class.getDeclaredConstructor(ItemStack.class, sourceType);
        constructor.setAccessible(true);
        return constructor.newInstance(stack, source);
    }

    private static CompoundTag snapshot(IItemHandler handler, RegistryOps<Tag> ops) {
        CompoundTag result = new CompoundTag(); result.putInt("Size", handler.getSlots());
        ListTag items = new ListTag(), limits = new ListTag();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = (CompoundTag) CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, stack).getOrThrow();
                rejectMissing(item); item.putInt("Slot", slot); items.add(item);
            }
            limits.add(IntTag.valueOf(handler.getSlotLimit(slot)));
        }
        result.put("Items", items); result.put("slot_limits", limits); return result;
    }

    private static void rejectMissing(Tag value) {
        if (value instanceof CompoundTag compound) {
            if (compound.getCompound("components").contains("ae2:missing_content_error")) throw new IllegalStateException("Missing nested AE content");
            for (String key : compound.getAllKeys()) rejectMissing(compound.get(key));
        } else if (value instanceof ListTag list) for (Tag child : list) rejectMissing(child);
    }

    private static Map<String, Long> inventoryKeys(CompoundTag data) {
        Map<String, Long> result = new HashMap<>();
        for (Tag value : data.getCompound("inventory").getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.getAllKeys().equals(Set.of("key", "amount")) || !entry.contains("amount", Tag.TAG_LONG) || entry.getLong("amount") <= 0)
                throw new IllegalStateException("Universal cell has invalid persistent entry");
            if (result.put(persistentKey(entry.getCompound("key")), entry.getLong("amount")) != null)
                throw new IllegalStateException("Universal cell has duplicate persistent keys");
        }
        return result;
    }

    private static String persistentKey(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            List<String> fields = new ArrayList<>();
            for (String key : new TreeSet<>(compound.getAllKeys()))
                fields.add(StringTag.valueOf(key) + ":" + persistentKey(compound.get(key)));
            return "10:{" + String.join(",", fields) + "}";
        }
        if (tag instanceof ListTag list) {
            List<String> values = new ArrayList<>();
            for (Tag child : list) values.add(persistentKey(child));
            return "9:[" + String.join(",", values) + "]";
        }
        return tag.getId() + ":" + tag;
    }
}
