package dev.szx.dimensionworks.migration;

import com.google.gson.Gson;
import net.minecraft.nbt.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** 将完整审计过的载荷写入第二份世界副本；保留不可启动标志，等待全档离线对账。 */
public final class OfflineWorldOverlay {
    private static final Gson JSON = new Gson();
    private static final Pattern BE_LOCATION = Pattern.compile("(.+\\.mca)#(\\d+)/block_entities\\[(\\d+)]");
    private static final Pattern ENTITY_LOCATION = Pattern.compile("(.+\\.mca)#(\\d+)");

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("base .partial world / new .partial world / complete overlay.nbt / new report.jsonl");
        Path base = Path.of(args[0]).toRealPath(), target = Path.of(args[1]).toAbsolutePath().normalize();
        Path planPath = Path.of(args[2]).toRealPath(), report = Path.of(args[3]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(base.resolve(".migration-incomplete")) || Files.exists(target) || Files.exists(report) ||
            target.startsWith(base) || base.startsWith(target) || report.startsWith(base) || planPath.startsWith(base) ||
            !target.getFileName().toString().endsWith(".partial")) throw new IllegalArgumentException("Unsafe or previous world overlay");
        CompoundTag plan = NbtIo.readCompressed(planPath, NbtAccounter.unlimitedHeap());
        if (plan.getInt("format") != 1 || !plan.getBoolean("complete") || plan.getInt("target_data_version") != 3955)
            throw new IllegalArgumentException("Incomplete or wrong-version overlay input");
        if (!plan.getString("retention_policy").equals("industrial_storage_players_v1"))
            throw new IllegalArgumentException("Unapproved world retention policy");
        Set<String> keptBlocks = strings(plan, "kept_block_ids"), keptActors = strings(plan, "kept_entity_ids");
        if (keptBlocks.isEmpty() || keptActors.isEmpty()) throw new IllegalArgumentException("Incomplete industrial retention catalogue");
        Map<CompoundTag, CompoundTag> states = new HashMap<>();
        for (Tag value : plan.getList("block_state_rows", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value, old = row.getCompound("source"), now = row.getCompound("data");
            String id = old.getString("Name"); boolean preserve = keptBlocks.contains(id);
            if (id.isEmpty() || (id.startsWith("minecraft:") && !preserve) ||
                (preserve && !old.equals(now)) || (!preserve && !now.equals(air())) || states.put(old, now) != null)
                throw new IllegalArgumentException("Incomplete or destructive block-state retention mapping");
        }
        Map<String, String> hashes = new TreeMap<>();
        for (Tag value : plan.getList("base_files", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value; String path = relative(row.getString("path"));
            if (!row.getString("sha256").matches("[0-9a-f]{64}") || hashes.put(path, row.getString("sha256")) != null)
                throw new IllegalArgumentException("Invalid or duplicate base fingerprint");
        }
        List<Path> paths;
        try (var walk = Files.walk(base)) {
            paths = walk.sorted().toList();
            if (paths.stream().anyMatch(Files::isSymbolicLink)) throw new IllegalArgumentException("Base world contains links");
        }
        Set<String> actual = new TreeSet<>();
        for (Path file : paths) if (Files.isRegularFile(file)) {
            String path = base.relativize(file).toString(); actual.add(path);
            if (!Objects.equals(hashes.get(path), hash(file))) throw new IOException("Base file fingerprint changed: " + path);
        }
        if (!actual.equals(hashes.keySet())) throw new IOException("Incomplete base file fingerprint coverage");

        Map<String, Map<Integer, Map<Integer, CompoundTag>>> tiles = new HashMap<>();
        int beCount = 0, retiredBeCount = 0;
        for (Tag value : plan.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value; var match = BE_LOCATION.matcher(row.getString("location"));
            if (!match.matches() || !row.contains("source", Tag.TAG_COMPOUND) ||
                (row.getBoolean("retired") ? row.contains("data") : !row.contains("data", Tag.TAG_COMPOUND)))
                throw new IllegalArgumentException("Invalid prepared block entity location or source");
            String path = relative(match.group(1)); int chunk = Integer.parseInt(match.group(2)), index = Integer.parseInt(match.group(3));
            if (chunk < 0 || chunk >= 1024 || !hashes.containsKey(path) || !path.contains("region/"))
                throw new IllegalArgumentException("Block entity outside the approved base");
            if (tiles.computeIfAbsent(path, p -> new HashMap<>()).computeIfAbsent(chunk, p -> new TreeMap<>()).put(index, row) != null)
                throw new IllegalArgumentException("Duplicate block entity overlay");
            beCount++;
            if (row.getBoolean("retired")) retiredBeCount++;
        }
        Map<String, Map<Integer, CompoundTag>> entities = new HashMap<>(); int entityCount = 0, retiredEntityCount = 0;
        for (Tag value : plan.getList("entity_chunks", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value; var match = ENTITY_LOCATION.matcher(row.getString("label"));
            if (!match.matches() || !row.contains("source", Tag.TAG_COMPOUND) || !row.contains("data", Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Invalid prepared entity chunk");
            String path = relative(match.group(1)); int index = Integer.parseInt(match.group(2));
            if (index < 0 || index >= 1024 || !hashes.containsKey(path) || !path.contains("entities/"))
                throw new IllegalArgumentException("Entity chunk outside the approved base");
            if (entities.computeIfAbsent(path, p -> new HashMap<>()).put(index, row) != null)
                throw new IllegalArgumentException("Duplicate entity chunk overlay");
            retiredEntityCount += validateActors(row, keptActors);
            entityCount += row.getCompound("data").getList("Entities", Tag.TAG_COMPOUND).size();
        }
        Map<String, CompoundTag> files = new TreeMap<>(); Set<String> omit = new TreeSet<>();
        for (Tag value : plan.getList("files", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value; String path = relative(row.getString("path"));
            if (!row.contains("data", Tag.TAG_COMPOUND) ||
                !(path.endsWith(".dat") || path.endsWith(".dat_old") || path.endsWith(".nbt")) || files.put(path, row.getCompound("data")) != null)
                throw new IllegalArgumentException("Invalid or duplicate complete NBT file");
        }
        for (Tag value : plan.getList("omit_files", Tag.TAG_STRING)) {
            String path = relative(value.getAsString());
            if (!hashes.containsKey(path) || !omit.add(path) || files.containsKey(path))
                throw new IllegalArgumentException("Invalid or duplicate retired file");
        }
        if (!files.containsKey("level.dat") || !omit.contains(".migration-incomplete") ||
            beCount != plan.getInt("expected_block_entities") || entityCount != plan.getInt("expected_entities") ||
            retiredBeCount != plan.getInt("expected_retired_block_entities") ||
            retiredEntityCount != plan.getInt("expected_retired_entities") ||
            entities.values().stream().mapToInt(Map::size).sum() != plan.getInt("expected_entity_chunks"))
            throw new IllegalArgumentException("Full overlay cardinality or required metadata is incomplete");
        for (String path : omit) if (tiles.containsKey(path) || entities.containsKey(path))
            throw new IllegalArgumentException("Cannot retire a region that contains protected overlays");

        Files.createDirectory(target, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        privateFile(target.resolve(".migration-incomplete"));
        Files.writeString(target.resolve(".migration-incomplete"), "payload_overlay_in_progress; do_not_start_minecraft\n");
        privateFile(report);
        long regions = 0, chunks = 0, appliedTiles = 0, appliedEntityChunks = 0;
        long[] dummyMarkers = new long[2]; // Minecraft 的无库存待初始化标记：保留 / 范围外归档。
        try (var log = Files.newBufferedWriter(report, StandardOpenOption.APPEND)) {
            for (Path source : paths) {
                String path = base.relativize(source).toString(); Path output = target.resolve(path);
                if (Files.isDirectory(source)) {
                    if (!path.isEmpty()) Files.createDirectories(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                    continue;
                }
                if (omit.contains(path)) {
                    log.write(JSON.toJson(Map.of("path", path, "operation", "approved_omission", "source_sha256", hashes.get(path)))); log.newLine();
                    continue;
                }
                if (path.endsWith(".mcc") || files.containsKey(path)) continue;
                if (!path.endsWith(".mca")) {
                    Files.copy(source, output); Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("rw-------")); continue;
                }
                Map<Integer, Map<Integer, CompoundTag>> regionTiles = tiles.getOrDefault(path, Map.of());
                Map<Integer, CompoundTag> regionEntities = entities.getOrDefault(path, Map.of());
                Set<Integer> usedTileChunks = new HashSet<>(), usedEntityChunks = new HashSet<>();
                long count = OfflineRegionDfu.convertRegion(source, output, (index, original) -> {
                    if (original.getInt("DataVersion") != 3955) throw new IOException("Base chunk is not fully DFU-prepared");
                    CompoundTag data = original.copy();
                    if (source.getParent().getFileName().toString().equals("region")) {
                        ListTag old = original.getList("block_entities", Tag.TAG_COMPOUND), converted = new ListTag();
                        Map<Integer, CompoundTag> prepared = regionTiles.getOrDefault(index, Map.of());
                        if (old.size() < prepared.size()) throw new IOException("Incomplete block entity overlay: " + path + "#" + index);
                        Set<Integer> usedTileIndexes = new HashSet<>();
                        for (int i = 0; i < old.size(); i++) {
                            CompoundTag row = prepared.get(i), tile = old.getCompound(i);
                            String block = blockAt(original, tile);
                            if (row == null) {
                                if (!tile.getString("id").equals("DUMMY") || !tile.getAllKeys().equals(Set.of("id", "x", "y", "z")))
                                    throw new IOException("Unindexed real block entity: " + path + "#" + index + "/block_entities[" + i + "]");
                                boolean keep = keptBlocks.contains(block); dummyMarkers[keep ? 0 : 1]++;
                                if (keep) converted.add(tile.copy());
                                log.write(JSON.toJson(Map.of("location", path + "#" + index + "/block_entities[" + i + "]",
                                    "operation", keep ? "retain_vanilla_pending_marker" : "retire_outside_scope_pending_marker",
                                    "block", block, "source_snbt", tile.toString()))); log.newLine();
                                continue;
                            }
                            usedTileIndexes.add(i);
                            if (!tile.equals(row.getCompound("source"))) throw new IOException("Original block entity differs from approved source");
                            if (row.getBoolean("retired") == keptBlocks.contains(block))
                                throw new IOException("Block entity retirement differs from the actual retained block");
                            if (row.getBoolean("retired")) continue;
                            CompoundTag now = row.getCompound("data");
                            for (String key : List.of("x", "y", "z"))
                                if (old.getCompound(i).getInt(key) != now.getInt(key)) throw new IOException("Converted block entity moved");
                            converted.add(now.copy());
                        }
                        if (!usedTileIndexes.equals(prepared.keySet())) throw new IOException("Prepared block entities were not applied exactly once");
                        if (!prepared.isEmpty()) usedTileChunks.add(index);
                        if (original.contains("block_entities")) data.put("block_entities", converted);
                        boolean changed = false;
                        for (Tag value : data.getList("sections", Tag.TAG_COMPOUND)) {
                            CompoundTag section = (CompoundTag) value;
                            ListTag palette = section.getCompound("block_states").getList("palette", Tag.TAG_COMPOUND);
                            for (int i = 0; i < palette.size(); i++) {
                                CompoundTag mapped = states.get(palette.getCompound(i));
                                if (mapped == null) throw new IOException("Unaccounted base block state");
                                if (!mapped.equals(palette.getCompound(i))) { palette.set(i, mapped.copy()); changed = true; }
                            }
                        }
                        if (changed) {
                            data.remove("Heightmaps"); data.putBoolean("isLightOn", false);
                            for (Tag value : data.getList("sections", Tag.TAG_COMPOUND)) {
                                CompoundTag section = (CompoundTag) value; section.remove("SkyLight"); section.remove("BlockLight");
                            }
                        }
                        retainTicks(data, "block_ticks", keptBlocks, false);
                        retainTicks(data, "fluid_ticks", keptBlocks, true);
                    } else if (source.getParent().getFileName().toString().equals("entities")) {
                        CompoundTag row = regionEntities.get(index);
                        if (row == null || !Objects.equals(original.get("Entities"), row.getCompound("source").get("Entities")) ||
                            !Objects.equals(original.get("Position"), row.getCompound("data").get("Position")))
                            throw new IOException("Incomplete or changed physical entity chunk overlay");
                        data = row.getCompound("data").copy(); usedEntityChunks.add(index);
                    }
                    promoteBrewing(data); return data;
                });
                if (!usedTileChunks.equals(regionTiles.keySet()) || !usedEntityChunks.equals(regionEntities.keySet()))
                    throw new IOException("Prepared region records were not applied exactly once");
                appliedTiles += regionTiles.values().stream().mapToInt(Map::size).sum(); appliedEntityChunks += usedEntityChunks.size();
                regions++; chunks += count;
                log.write(JSON.toJson(Map.of("path", path, "operation", "complete_payload_overlay", "chunks", count, "target_sha256", hash(output)))); log.newLine(); log.flush();
                if (regions % 50 == 0) System.out.println("WORLD_OVERLAY_PROGRESS regions=" + regions + " chunks=" + chunks);
            }
            for (Map.Entry<String, CompoundTag> entry : files.entrySet()) {
                Path output = target.resolve(entry.getKey()); Files.createDirectories(output.getParent(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                privateFile(output); NbtIo.writeCompressed(entry.getValue(), output);
                log.write(JSON.toJson(Map.of("path", entry.getKey(), "operation", "complete_nbt_file", "target_sha256", hash(output)))); log.newLine();
            }
            for (Path source : paths) if (source.toString().endsWith(".mcc") && !omit.contains(base.relativize(source).toString())) {
                Path output = target.resolve(base.relativize(source));
                if (!Files.exists(output)) { Files.copy(source, output); Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("rw-------")); }
            }
            if (appliedTiles != beCount || appliedEntityChunks != plan.getInt("expected_entity_chunks") || chunks != plan.getLong("expected_output_chunks"))
                throw new IOException("Whole-world overlay completion counts differ from the approved plan");
            Files.writeString(target.resolve(".migration-incomplete"), "payload_overlay_complete; full_offline_audit_required; do_not_start_minecraft\n");
            log.write(JSON.toJson(Map.of("payload_overlay_complete", true, "regions", regions, "chunks", chunks,
                "block_entities", appliedTiles - retiredBeCount, "retired_block_entities", retiredBeCount,
                "entity_chunks", appliedEntityChunks, "entities", entityCount,
                "ready_for_minecraft", false, "base_world_unchanged", true))); log.newLine();
            log.write(JSON.toJson(Map.of("retained_pending_markers", dummyMarkers[0], "retired_pending_markers", dummyMarkers[1]))); log.newLine();
        }
        System.out.println("WORLD_OVERLAY_DONE chunks=" + chunks + " entities=" + entityCount + " ready_for_minecraft=false");
    }

    private static void promoteBrewing(CompoundTag data) throws IOException {
        if (!data.contains("brewinandchewin:data_version")) return;
        if (!data.contains("brewinandchewin:data_version", Tag.TAG_INT) ||
            (data.getInt("brewinandchewin:data_version") != 2 && data.getInt("brewinandchewin:data_version") != 100))
            throw new IOException("Unexpected brewing file metadata");
        data.putInt("brewinandchewin:data_version", 100);
    }
    private static int validateActors(CompoundTag row, Set<String> keptActors) throws IOException {
        Map<UUID, CompoundTag> source = new HashMap<>(); Set<UUID> retained = new HashSet<>(), retired = new HashSet<>();
        for (Tag value : row.getCompound("source").getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) value;
            if (!actor.hasUUID("UUID") || source.put(actor.getUUID("UUID"), actor) != null)
                throw new IOException("Source entity chunk has missing or duplicate identities");
        }
        for (Tag value : row.getList("retired_actor_uuids", Tag.TAG_INT_ARRAY)) {
            if (!(value instanceof IntArrayTag words) || words.getAsIntArray().length != 4)
                throw new IOException("Invalid retired actor identity");
            CompoundTag identity = new CompoundTag(); identity.put("UUID", words.copy()); UUID uuid = identity.getUUID("UUID");
            CompoundTag old = source.get(uuid);
            if (old == null || !retired.add(uuid) || actorTreeProtected(old, keptActors))
                throw new IOException("Retirement would remove an industrial actor or its carrier");
        }
        for (Tag value : row.getCompound("data").getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) value;
            if (!actor.hasUUID("UUID")) throw new IOException("Converted actor lost its identity");
            UUID uuid = actor.getUUID("UUID"); CompoundTag old = source.get(uuid);
            if (old == null || retired.contains(uuid) || !retained.add(uuid)) throw new IOException("Converted actors changed their identity set");
            if (!actorTreeProtected(old, keptActors)) throw new IOException("Unrelated world actor was retained outside the limited policy");
            sameActorTree(old, actor);
        }
        if (retained.size() + retired.size() != source.size()) throw new IOException("Unaccounted source actor omission");
        return retired.size();
    }
    private static void sameActorTree(CompoundTag source, CompoundTag target) throws IOException {
        if (!source.getUUID("UUID").equals(target.getUUID("UUID")) || !source.getString("id").equals(target.getString("id")))
            throw new IOException("Converted actor type or passenger identity changed");
        ListTag before = source.getList("Passengers", Tag.TAG_COMPOUND), after = target.getList("Passengers", Tag.TAG_COMPOUND);
        if (before.size() != after.size()) throw new IOException("Converted passenger count changed");
        for (int i = 0; i < before.size(); i++) sameActorTree(before.getCompound(i), after.getCompound(i));
    }
    private static boolean actorTreeProtected(CompoundTag actor, Set<String> keptActors) {
        if (actor.getString("id").equals("minecraft:player")) return true;
        if (keptActors.contains(actor.getString("id"))) return true;
        for (Tag child : actor.getList("Passengers", Tag.TAG_COMPOUND))
            if (actorTreeProtected((CompoundTag) child, keptActors)) return true;
        return false;
    }
    private static Set<String> strings(CompoundTag root, String key) {
        Set<String> result = new HashSet<>();
        for (Tag value : root.getList(key, Tag.TAG_STRING))
            if (!value.getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !result.add(value.getAsString()))
                throw new IllegalArgumentException("Invalid or duplicate retention ID");
        return result;
    }
    private static CompoundTag air() { CompoundTag value = new CompoundTag(); value.putString("Name", "minecraft:air"); return value; }
    private static String blockAt(CompoundTag root, CompoundTag tile) throws IOException {
        int x = tile.getInt("x"), y = tile.getInt("y"), z = tile.getInt("z");
        if (x >> 4 != root.getInt("xPos") || z >> 4 != root.getInt("zPos")) throw new IOException("Block entity is outside its source chunk");
        for (Tag value : root.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = (CompoundTag) value;
            if (section.getByte("Y") != y >> 4) continue;
            CompoundTag states = section.getCompound("block_states"); ListTag palette = states.getList("palette", Tag.TAG_COMPOUND);
            if (palette.isEmpty()) throw new IOException("Missing source palette");
            int slot = 0;
            if (palette.size() > 1) {
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1)), perWord = 64 / bits;
                int position = (y & 15) * 256 + (z & 15) * 16 + (x & 15); long[] words = states.getLongArray("data");
                if (words.length != (4096 + perWord - 1) / perWord) throw new IOException("Invalid source palette data");
                slot = (int) ((words[position / perWord] >>> (position % perWord * bits)) & ((1L << bits) - 1));
                if (slot >= palette.size()) throw new IOException("Source palette index is out of range");
            }
            return palette.getCompound(slot).getString("Name");
        }
        throw new IOException("No source block section for the block entity");
    }
    private static void retainTicks(CompoundTag root, String key, Set<String> keptBlocks, boolean fluid) throws IOException {
        if (!root.contains(key)) return;
        ListTag result = new ListTag();
        for (Tag value : root.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag tick = (CompoundTag) value; String id = tick.getString("i");
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IOException("Malformed scheduled tick ID");
            // 原版流体及工业命名空间的流体仍可运行；移除范围外流体的待执行更新。
            boolean keep = keptBlocks.contains(id) || id.startsWith("minecraft:");
            if (fluid && !keep) {
                String prefix = id.substring(0, Math.max(0, id.indexOf(':') + 1));
                keep = !prefix.isEmpty() && keptBlocks.stream().anyMatch(block -> block.startsWith(prefix));
            }
            if (keep) result.add(tick.copy());
        }
        root.put(key, result);
    }
    private static String relative(String value) {
        Path path = Path.of(value).normalize();
        if (value.isEmpty() || path.isAbsolute() || path.startsWith("..") || !path.toString().equals(value))
            throw new IllegalArgumentException("Unsafe or noncanonical world-relative path");
        return value;
    }
    private static void privateFile(Path path) throws IOException {
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }
    private static String hash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) { byte[] buffer = new byte[65536]; for (int size; (size = stream.read(buffer)) >= 0;) digest.update(buffer, 0, size); }
        return HexFormat.of().formatHex(digest.digest());
    }
}
