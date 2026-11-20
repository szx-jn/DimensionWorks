package dev.szx.dimensionworks.migration.worlddata;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.UUID;

/** 维度附件与票据的隔离序列化；只读取控制器注册表，不应用票据或写入真实维度。 */
@Mod("dimensionworks_migration_world_data_batch")
public final class NativeWorldDataBatch {
    public NativeWorldDataBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated world data fixture");
            Path output = cwd.resolve("local/migration-world-data-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse previous world data output");
            CompoundTag input = NbtIo.readCompressed(cwd.resolve("local/migration-world-data-input.nbt"), NbtAccounter.unlimitedHeap());
            if (input.getInt("format") != 1) throw new IllegalStateException("Wrong world data format");
            var registry = event.getServer().registryAccess();
            var field = ForcedChunkManager.class.getDeclaredField("controllers"); field.setAccessible(true);
            Map<ResourceLocation, ?> controllers = (Map<ResourceLocation, ?>) field.get(null);
            CompoundTag result = new CompoundTag(); result.putInt("format", 1);
            ListTag names = new ListTag();
            controllers.keySet().stream().map(Object::toString).sorted().forEach(id -> names.add(StringTag.valueOf(id)));
            result.put("ticket_controllers", names);
            System.out.println("MIGRATION_TICKET_CONTROLLERS " + names);
            CompoundTag runtimeBounds = new CompoundTag();
            for (var level : event.getServer().getAllLevels()) {
                CompoundTag entry = new CompoundTag(); entry.putInt("min_y", level.dimensionType().minY());
                entry.putInt("height", level.dimensionType().height());
                entry.putDouble("coordinate_scale", level.dimensionType().coordinateScale());
                runtimeBounds.put(level.dimension().location().toString(), entry);
            }
            result.put("runtime_dimension_bounds", runtimeBounds);
            if (input.contains("world_gen_settings", Tag.TAG_COMPOUND)) {
                var ops = RegistryOps.create(NbtOps.INSTANCE, registry);
                CompoundTag prepared = input.getCompound("world_gen_settings").copy();
                var options = WorldOptions.CODEC.codec().parse(ops, prepared).getOrThrow();
                var dimensions = WorldDimensions.CODEC.codec().parse(ops, prepared).getOrThrow();
                CompoundTag saved = (CompoundTag) WorldOptions.CODEC.codec().encodeStart(ops, options).getOrThrow();
                saved.merge((CompoundTag) WorldDimensions.CODEC.codec().encodeStart(ops, dimensions).getOrThrow());
                var second = WorldDimensions.CODEC.codec().parse(ops, saved).getOrThrow();
                Tag again = WorldDimensions.CODEC.codec().encodeStart(ops, second).getOrThrow();
                if (!saved.get("dimensions").equals(((CompoundTag) again).get("dimensions")))
                    throw new IllegalStateException("World dimensions did not stabilize on reload");
                result.put("world_gen_settings", saved);
                CompoundTag bounds = new CompoundTag();
                dimensions.dimensions().forEach((key, stem) -> {
                    var type = stem.type().value();
                    CompoundTag entry = new CompoundTag(); entry.putInt("min_y", type.minY());
                    entry.putInt("height", type.height()); entry.putDouble("coordinate_scale", type.coordinateScale());
                    entry.putString("type", stem.type().unwrapKey().orElseThrow().location().toString());
                    bounds.put(key.location().toString(), entry);
                });
                result.put("dimension_bounds", bounds);
                System.out.println("MIGRATION_WORLD_GEN_DONE dimensions=" + dimensions.dimensions().size());
            }
            if (input.contains("forced_chunks", Tag.TAG_COMPOUND)) {
                CompoundTag prepared = input.getCompound("forced_chunks").copy();
                if (prepared.contains("ForgeForced") || !prepared.contains("ModForced", Tag.TAG_LIST))
                    throw new IllegalStateException("Forced chunk payload has not been prepared for target controllers");
                ListTag tickets = prepared.getList("ModForced", Tag.TAG_COMPOUND);
                for (Tag value : tickets) {
                    String id = ((CompoundTag) value).getString("Controller");
                    if (!controllers.containsKey(ResourceLocation.parse(id)))
                        throw new IllegalStateException("Source ticket controller is absent: " + id);
                }
                ForcedChunksSavedData first = ForcedChunksSavedData.load(prepared, registry);
                CompoundTag saved = first.save(new CompoundTag(), registry);
                ForcedChunksSavedData second = ForcedChunksSavedData.load(saved.copy(), registry);
                CompoundTag reloaded = second.save(new CompoundTag(), registry);
                result.put("forced_chunks", saved); result.put("forced_chunks_reloaded", reloaded);
            }
            Registry<AttachmentType<?>> attachments = (Registry) BuiltInRegistries.REGISTRY.get(ResourceLocation.parse("neoforge:attachment_types"));
            var serializerField = AttachmentType.class.getDeclaredField("serializer"); serializerField.setAccessible(true);
            ListTag rows = new ListTag();
            for (Tag value : input.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value;
                var level = event.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(row.getString("dimension"))));
                if (level == null) throw new IllegalStateException("Source attachment dimension absent from target");
                CompoundTag saved = new CompoundTag(), reloaded = new CompoundTag();
                for (String id : row.getCompound("data").getAllKeys()) {
                    if (!attachments.containsKey(ResourceLocation.parse(id))) throw new IllegalStateException("Missing attachment " + id);
                    IAttachmentSerializer serializer = (IAttachmentSerializer) serializerField.get(attachments.get(ResourceLocation.parse(id)));
                    if (serializer == null) throw new IllegalStateException("Attachment is not persistent: " + id);
                    Object first = serializer.read(level, row.getCompound("data").get(id).copy(), registry);
                    Tag data = serializer.write(first, registry);
                    if (data == null) throw new IllegalStateException("Attachment serializer omitted source state: " + id);
                    Tag again = serializer.write(serializer.read(level, data.copy(), registry), registry);
                    if (!data.equals(again)) throw new IllegalStateException("Attachment did not stabilize on reload: " + id);
                    saved.put(id, data); reloaded.put(id, again);
                }
                CompoundTag converted = new CompoundTag(); converted.putString("label", row.getString("label"));
                converted.put("data", saved); converted.put("reloaded_data", reloaded); rows.add(converted);
            }
            result.put("rows", rows); result.putBoolean("source_world_opened", false);
            ListTag savedRows = new ListTag();
            for (Tag value : input.getList("saved_data", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value;
                String kind = row.getString("kind");
                SavedData first = loadSavedData(kind, row.getCompound("data").copy(), event.getServer());
                CompoundTag saved = first.save(new CompoundTag(), registry);
                SavedData second = loadSavedData(kind, saved.copy(), event.getServer());
                CompoundTag reloaded = second.save(new CompoundTag(), registry);
                if (!saved.equals(reloaded)) throw new IllegalStateException("SavedData did not stabilize: " + kind);
                CompoundTag converted = new CompoundTag(); converted.putString("label", row.getString("label"));
                converted.putString("kind", kind); converted.put("data", saved); converted.put("reloaded_data", reloaded);
                if (kind.equals("ae_players")) {
                    long[] uuids = row.getCompound("data").getLongArray("profileIds");
                    int[] ids = row.getCompound("data").getIntArray("playerIds");
                    if (uuids.length != ids.length * 2) throw new IllegalStateException("Invalid source AE2 identity arrays");
                    var contract = Class.forName("appeng.api.features.IPlayerRegistry");
                    var getProfile = contract.getMethod("getProfileId", int.class);
                    ListTag bindings = new ListTag();
                    for (int i = 0; i < ids.length; i++) {
                        UUID expected = new UUID(uuids[i * 2], uuids[i * 2 + 1]);
                        if (!expected.equals(getProfile.invoke(first, ids[i])) || !expected.equals(getProfile.invoke(second, ids[i])))
                            throw new IllegalStateException("AE2 player/network identity binding changed");
                        CompoundTag binding = new CompoundTag(); binding.putUUID("uuid", expected);
                        binding.putInt("id", ids[i]); bindings.add(binding);
                    }
                    converted.put("bindings", bindings);
                }
                savedRows.add(converted);
            }
            result.put("saved_data", savedRows);
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(result, output);
            System.out.println("MIGRATION_WORLD_DATA_BATCH_DONE rows=" + rows.size() + " controllers=" + names.size() + " saved_data=" + savedRows.size());
        } catch (Exception error) { throw new RuntimeException("Native world data serialization batch failed", error); }
    }

    private static SavedData loadSavedData(String kind, CompoundTag data, net.minecraft.server.MinecraftServer server) throws Exception {
        if (kind.equals("ae_players")) {
            var type = Class.forName("appeng.api.features.PlayerRegistryInternal");
            var load = type.getDeclaredMethod("load", net.minecraft.server.MinecraftServer.class, CompoundTag.class);
            load.setAccessible(true); return (SavedData) load.invoke(null, server, data);
        }
        String type = switch (kind) {
            case "territories" -> "dev.szx.dimensionworks.transfer.territory.TerritorySavedData";
            case "tutorial" -> "dev.szx.dimensionworks.transfer.tutorial.TutorialSavedData";
            case "return_points" -> "dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData";
            case "logistics_channels" -> "dev.szx.dimensionworks.mekstress.memory.ChannelDirectorySavedData";
            case "logistics_stock" -> "dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData";
            case "molten_positions" -> "dev.szx.dimensionworks.cavefactory.MoltenCrystallizationData";
            default -> throw new IllegalArgumentException("Unapproved SavedData native loader: " + kind);
        };
        boolean provider = kind.equals("logistics_stock") || kind.equals("molten_positions");
        var load = provider ? Class.forName(type).getDeclaredMethod("load", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class)
                            : Class.forName(type).getDeclaredMethod("load", CompoundTag.class);
        load.setAccessible(true);
        return (SavedData) (provider ? load.invoke(null, data, server.registryAccess()) : load.invoke(null, data));
    }
}
