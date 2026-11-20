package dev.szx.dimensionworks.migration.entitydata;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.TreeMap;

/** 实际实体载荷的隔离读写；不把实体加入世界或执行其 tick。 */
@Mod("dimensionworks_migration_entity_batch")
public final class NativeEntityBatch {
    private MinecraftServer pending;
    public NativeEntityBatch(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(this::started); NeoForge.EVENT_BUS.addListener(this::tick);
    }
    private void started(ServerStartedEvent event) { pending = event.getServer(); }
    private void tick(ServerTickEvent.Post event) {
        if (pending != null) { MinecraftServer server = pending; pending = null; runBatch(server); }
    }

    private void runBatch(MinecraftServer server) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated entity fixture");
            Path output = cwd.resolve("local/migration-entity-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse previous native entity output");
            CompoundTag input = NbtIo.readCompressed(cwd.resolve("local/migration-entity-input.nbt"), NbtAccounter.unlimitedHeap());
            String phase = input.getString("diagnostic_id");
            if (input.getInt("format") != 1 || !phase.matches("[a-z0-9-]{1,64}")) throw new IllegalStateException("Invalid entity phase");
            Path diagnostics = cwd.resolve("local/migration-entity-errors-" + phase + ".log"); privateFile(diagnostics);
            ListTag rows = new ListTag(), errors = new ListTag();
            try (var log = new PrintWriter(Files.newBufferedWriter(diagnostics))) {
                for (Tag value : input.getList("rows", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) value;
                    CompoundTag failed = new CompoundTag(); failed.putString("label", row.getString("label"));
                    try {
                        CompoundTag data = row.getCompound("data");
                        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(row.getString("dimension"))));
                        if (level == null) throw new IllegalStateException("Source entity dimension absent from target");
                        Entity first = load(data, level);
                        CompoundTag saved = save(first);
                        failed.put("data", saved.copy());
                        Entity second = load(saved, level);
                        CompoundTag reloaded = save(second);
                        failed.put("reloaded_data", reloaded.copy());
                        boolean exact = saved.equals(reloaded);
                        if (!exact && !canonicalActor(saved).equals(canonicalActor(reloaded)))
                            throw new IllegalStateException("Entity persistence did not stabilize");
                        if (!data.getUUID("UUID").equals(first.getUUID()) || !saved.getUUID("UUID").equals(second.getUUID()))
                            throw new IllegalStateException("Entity UUID changed");
                        CompoundTag converted = new CompoundTag(); converted.putString("label", row.getString("label"));
                        converted.put("data", saved); converted.put("reloaded_data", reloaded); rows.add(converted);
                        converted.putBoolean("attribute_order_changed", !exact);
                    } catch (Exception error) {
                        log.println("ROW " + row.getString("label")); error.printStackTrace(log); log.flush();
                        failed.putString("error", error.toString()); errors.add(failed);
                        if (!input.getBoolean("collect_errors")) throw error;
                    }
                    if ((rows.size() + errors.size()) % 2000 == 0)
                        System.out.println("MIGRATION_ENTITY_BATCH_PROGRESS ready=" + rows.size() + " errors=" + errors.size());
                }
            }
            CompoundTag result = new CompoundTag(); result.putInt("format", 1); result.put("rows", rows); result.put("errors", errors);
            result.putBoolean("complete", errors.isEmpty()); result.putBoolean("source_world_opened", false);
            privateFile(output); NbtIo.writeCompressed(result, output);
            if (!errors.isEmpty()) throw new IllegalStateException("Native entity batch errors=" + errors.size() + "; see private diagnostics");
            System.out.println("MIGRATION_ENTITY_BATCH_DONE rows=" + rows.size());
        } catch (Exception error) { throw new RuntimeException("Native entity persistence batch failed", error); }
    }
    private static Entity load(CompoundTag data, ServerLevel level) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(data.getString("id"))))
            throw new IllegalStateException("Missing actual entity type");
        Entity result = EntityType.loadEntityRecursive(data.copy(), level, entity -> entity);
        if (result == null) throw new IllegalStateException("Native entity loader omitted source entity");
        return result;
    }
    private static CompoundTag save(Entity entity) {
        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        data.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()); return data;
    }
    private static CompoundTag canonicalActor(CompoundTag source) {
        CompoundTag result = source.copy();
        if (source.contains("attributes", Tag.TAG_LIST)) {
            TreeMap<String, CompoundTag> attributes = new TreeMap<>();
            for (Tag value : source.getList("attributes", Tag.TAG_COMPOUND)) {
                CompoundTag attribute = (CompoundTag) value;
                if (!attribute.contains("id", Tag.TAG_STRING) || attributes.put(attribute.getString("id"), attribute) != null)
                    throw new IllegalStateException("Entity has duplicate or unnamed persisted attributes");
            }
            ListTag ordered = new ListTag(); attributes.values().forEach(value -> ordered.add(value.copy()));
            result.put("attributes", ordered);
        }
        if (source.contains("Passengers", Tag.TAG_LIST)) {
            ListTag passengers = new ListTag();
            for (Tag value : source.getList("Passengers", Tag.TAG_COMPOUND)) passengers.add(canonicalActor((CompoundTag) value));
            result.put("Passengers", passengers);
        }
        return result;
    }
    private static void privateFile(Path path) throws java.io.IOException {
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }
}
