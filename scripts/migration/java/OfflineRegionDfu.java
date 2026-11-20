package dev.szx.dimensionworks.migration;

import com.google.gson.Gson;
import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

/** 唯一旧档的原版区域基础转换；保留原资产等待已验证的模组载荷覆盖，输出始终不可启动。 */
public final class OfflineRegionDfu {
    private static final int SECTOR = 4096;
    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final Gson JSON = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("sealed world / new .partial world / approved palettes / new report.jsonl");
        Path source = Path.of(args[0]).toRealPath(), target = Path.of(args[1]).toAbsolutePath().normalize();
        Path report = Path.of(args[3]).toAbsolutePath().normalize();
        if (!Files.isDirectory(source) || !Files.isRegularFile(source.resolve("level.dat")) ||
            Files.exists(target) || Files.exists(report) || target.startsWith(source) || source.startsWith(target) ||
            report.startsWith(source) || !target.getFileName().toString().endsWith(".partial"))
            throw new IllegalArgumentException("Refuse unsafe or previous region conversion");
        SharedConstants.tryDetectVersion();
        if (!SharedConstants.getCurrentVersion().getName().equals("1.21.1") ||
            SharedConstants.getCurrentVersion().getDataVersion().getVersion() != 3955)
            throw new IllegalStateException("Wrong fixed Minecraft DFU classpath");
        Map<CompoundTag, CompoundTag> palettes = new HashMap<>();
        CompoundTag approved = NbtIo.readCompressed(Path.of(args[2]), NbtAccounter.unlimitedHeap());
        if (approved.getInt("format") != 1) throw new IllegalArgumentException("Wrong palette map format");
        for (Tag value : approved.getList("rows", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            if (palettes.put(row.getCompound("source"), row.getCompound("data")) != null)
                throw new IllegalArgumentException("Duplicate approved source palette");
        }
        var fixer = DataFixers.getDataFixer();
        List<Path> paths;
        try (var walk = Files.walk(source)) {
            paths = walk.sorted().toList();
            if (paths.stream().anyMatch(Files::isSymbolicLink)) throw new IllegalArgumentException("Source contains links");
        }
        Files.createDirectory(target, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        privateFile(target.resolve(".migration-incomplete"));
        Files.writeString(target.resolve(".migration-incomplete"), "region_base_only; assets_and_level_not_migrated; do_not_start_minecraft\n");
        privateFile(report);
        long chunks = 0, regions = 0, empty = 0, copied = 0;
        try (var log = Files.newBufferedWriter(report, StandardOpenOption.APPEND)) {
            for (Path path : paths) {
                Path relative = source.relativize(path), output = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    if (!relative.toString().isEmpty()) Files.createDirectories(output,
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                    continue;
                }
                if (!Files.isRegularFile(path)) throw new IOException("Unknown source file type");
                if (path.getFileName().toString().equals("session.lock")) {
                    log.write(JSON.toJson(Map.of("path", relative.toString(), "operation", "omit_old_process_lock", "sha256", hash(path)))); log.newLine();
                    continue;
                }
                if (path.toString().endsWith(".mcc")) continue;
                if (!path.toString().endsWith(".mca")) {
                    Files.copy(path, output); Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("rw-------")); copied++;
                    continue;
                }
                String folder = path.getParent().getFileName().toString();
                DSL.TypeReference reference = switch (folder) {
                    case "region" -> References.CHUNK;
                    case "entities" -> References.ENTITY_CHUNK;
                    case "poi" -> References.POI_CHUNK;
                    default -> throw new IOException("Unclassified source region directory");
                };
                long count = convertRegion(path, output, (index, original) -> {
                    if (original.getInt("DataVersion") != 3465) throw new IOException("Source region is not the sealed 3465 baseline");
                    CompoundTag input = original.copy();
                    if (folder.equals("region")) {
                        for (Tag value : input.getList("sections", Tag.TAG_COMPOUND)) {
                            ListTag states = ((CompoundTag) value).getCompound("block_states").getList("palette", Tag.TAG_COMPOUND);
                            for (int i = 0; i < states.size(); i++) {
                                CompoundTag mapped = palettes.get(states.getCompound(i));
                                if (mapped == null) throw new IOException("No approved target block state");
                                states.set(i, mapped.copy());
                            }
                        }
                        input.remove("block_entities");
                    } else if (folder.equals("entities")) input.remove("Entities");
                    var fixed = fixer.update(reference, new Dynamic<>(NbtOps.INSTANCE, input), 3465, 3955).getValue();
                    if (!(fixed instanceof CompoundTag result)) throw new IOException("DFU result is not a compound");
                    if (folder.equals("region")) {
                        if (result.getInt("xPos") != original.getInt("xPos") || result.getInt("zPos") != original.getInt("zPos"))
                            throw new IOException("DFU changed source chunk position");
                        ListTag before = input.getList("sections", Tag.TAG_COMPOUND), after = result.getList("sections", Tag.TAG_COMPOUND);
                        if (before.size() != after.size()) throw new IOException("DFU changed section count");
                        for (int i = 0; i < before.size(); i++) {
                            CompoundTag old = before.getCompound(i), now = after.getCompound(i);
                            if (!old.get("Y").equals(now.get("Y")) || !Objects.equals(old.get("block_states"), now.get("block_states")))
                                throw new IOException("DFU changed approved section layout");
                        }
                        if (original.contains("block_entities")) result.put("block_entities", original.get("block_entities").copy());
                    } else if (folder.equals("entities")) {
                        if (!Objects.equals(original.get("Position"), result.get("Position"))) throw new IOException("DFU changed entity chunk position");
                        if (original.contains("Entities")) result.put("Entities", original.get("Entities").copy());
                    }
                    result.putInt("DataVersion", 3955);
                    return result;
                });
                regions++; chunks += count; if (count == 0) empty++;
                log.write(JSON.toJson(Map.of("path", relative.toString(), "operation", "region_base_only", "chunks", count,
                    "source_sha256", hash(path), "target_sha256", hash(output), "assets_require_overlay", true))); log.newLine(); log.flush();
                if (regions % 10 == 0) System.out.println("REGION_BASE_PROGRESS regions=" + regions + " chunks=" + chunks);
            }
            for (Path path : paths) if (Files.isRegularFile(path) && path.toString().endsWith(".mcc")) {
                Path output = target.resolve(source.relativize(path));
                if (!Files.exists(output)) { Files.copy(path, output); Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("rw-------")); }
            }
            log.write(JSON.toJson(Map.of("base_complete", true, "regions", regions, "chunks", chunks, "empty_regions", empty,
                "copied_nonregion_files", copied, "ready_for_minecraft", false, "source_world_opened", false))); log.newLine();
        }
        System.out.println("REGION_BASE_DONE regions=" + regions + " chunks=" + chunks + " ready_for_minecraft=false");
    }

    @FunctionalInterface interface Conversion { CompoundTag apply(int index, CompoundTag source) throws Exception; }

    static long convertRegion(Path source, Path target, Conversion conversion) throws Exception {
        privateFile(target);
        try (RandomAccessFile input = new RandomAccessFile(source.toFile(), "r"); RandomAccessFile output = new RandomAccessFile(target.toFile(), "rw")) {
            if (input.length() == 0) return 0;
            if (input.length() < 8192) throw new IOException("Truncated region header");
            byte[] header = new byte[8192]; input.readFully(header);
            output.write(new byte[4096]); output.write(header, 4096, 4096);
            BitSet used = new BitSet(); used.set(0, 2);
            int next = 2; long count = 0;
            for (int index = 0; index < 1024; index++) {
                int loc = java.nio.ByteBuffer.wrap(header, index * 4, 4).getInt(), offset = loc >>> 8, sectors = loc & 255;
                if (loc == 0) continue;
                int overlap = used.nextSetBit(offset);
                if (offset < 2 || sectors == 0 || (long) (offset + sectors) * SECTOR > input.length() + SECTOR - 1 ||
                    (overlap >= 0 && overlap < offset + sectors)) throw new IOException("Invalid or overlapping source sectors");
                used.set(offset, offset + sectors); input.seek((long) offset * SECTOR);
                int length = input.readInt(), kind = input.readUnsignedByte();
                if (length < 1 || length + 4 > sectors * SECTOR || input.getFilePointer() + length - 1 > input.length())
                    throw new IOException("Truncated source region payload");
                byte[] raw;
                if ((kind & 128) != 0) {
                    if (length != 1) throw new IOException("Invalid external source chunk pointer");
                    raw = Files.readAllBytes(external(source, index));
                } else { raw = new byte[length - 1]; input.readFully(raw); }
                InputStream bytes = new ByteArrayInputStream(raw);
                InputStream decoded = switch (kind & 127) {
                    case 1 -> new GZIPInputStream(bytes); case 2 -> new InflaterInputStream(bytes); case 3 -> bytes;
                    default -> throw new IOException("Unknown source compression");
                };
                CompoundTag old;
                try (var stream = new DataInputStream(decoded)) {
                    old = NbtIo.read(stream, NbtAccounter.unlimitedHeap());
                    if (stream.read() != -1) throw new IOException("Trailing source NBT data");
                }
                CompoundTag converted = conversion.apply(index, old);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (var stream = new DataOutputStream(new DeflaterOutputStream(buffer))) { NbtIo.write(converted, stream); }
                byte[] payload = buffer.toByteArray(); int allocation = (payload.length + 5 + SECTOR - 1) / SECTOR;
                output.seek((long) next * SECTOR);
                if (allocation > 255) {
                    Path file = external(target, index); privateFile(file); Files.write(file, payload);
                    output.writeInt(1); output.writeByte(130); allocation = 1;
                } else { output.writeInt(payload.length + 1); output.writeByte(2); output.write(payload); }
                output.setLength((long) (next + allocation) * SECTOR);
                output.seek(index * 4L); output.writeInt((next << 8) | allocation);
                next += allocation; count++;
            }
            return count;
        }
    }

    private static Path external(Path region, int index) throws IOException {
        var match = REGION.matcher(region.getFileName().toString());
        if (!match.matches()) throw new IOException("Invalid external region name");
        int x = Integer.parseInt(match.group(1)) * 32 + index % 32, z = Integer.parseInt(match.group(2)) * 32 + index / 32;
        return region.resolveSibling("c." + x + "." + z + ".mcc");
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
