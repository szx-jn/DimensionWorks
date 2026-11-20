package dev.szx.dimensionworks.migration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 固定 3465 → 3955 的离线原版 DFU，不加载世界或替代模组组件转换。 */
public final class VanillaDfuBridge {
    private static DSL.TypeReference reference(String name) {
        return switch (name) {
            case "item_stack" -> References.ITEM_STACK;
            case "player" -> References.PLAYER;
            case "entity" -> References.ENTITY;
            case "entity_chunk" -> References.ENTITY_CHUNK;
            case "block_entity" -> References.BLOCK_ENTITY;
            case "block_state" -> References.BLOCK_STATE;
            case "chunk" -> References.CHUNK;
            case "poi_chunk" -> References.POI_CHUNK;
            case "level" -> References.LEVEL;
            case "world_gen_settings" -> References.WORLD_GEN_SETTINGS;
            case "saved_data_map_data" -> References.SAVED_DATA_MAP_DATA;
            case "saved_data_scoreboard" -> References.SAVED_DATA_SCOREBOARD;
            case "saved_data_raids" -> References.SAVED_DATA_RAIDS;
            default -> throw new IllegalArgumentException("Unsupported DFU reference: " + name);
        };
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        var version = SharedConstants.getCurrentVersion();
        if (!version.getName().equals("1.21.1") || version.getDataVersion().getVersion() != 3955)
            throw new IllegalStateException("DFU classpath is not Minecraft 1.21.1 / DataVersion 3955");
        var fixer = DataFixers.getDataFixer();
        Gson gson = new Gson();
        System.out.println("DW_DFU_READY\t3955");
        try (var lines = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            for (String line; (line = lines.readLine()) != null;) {
                JsonObject request = gson.fromJson(line, JsonObject.class);
                JsonObject response = new JsonObject();
                response.add("sequence", request.get("sequence"));
                try {
                    byte[] bytes = Base64.getDecoder().decode(request.get("nbt").getAsString());
                    CompoundTag input;
                    try (var stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
                        input = NbtIo.read(stream, NbtAccounter.unlimitedHeap());
                        if (stream.available() != 0) throw new IOException("Trailing NBT data");
                    }
                    var fixed = fixer.update(reference(request.get("reference").getAsString()),
                            new Dynamic<>(NbtOps.INSTANCE, input), 3465, 3955).getValue();
                    if (!(fixed instanceof CompoundTag output)) throw new IOException("DFU result is not a compound");
                    var buffer = new ByteArrayOutputStream();
                    try (var stream = new DataOutputStream(buffer)) { NbtIo.write(output, stream); }
                    response.addProperty("nbt", Base64.getEncoder().encodeToString(buffer.toByteArray()));
                } catch (Exception error) {
                    response.addProperty("error", error.toString());
                }
                System.out.println("DW_DFU_RESULT\t" + gson.toJson(response));
                System.out.flush();
            }
        }
    }
}
