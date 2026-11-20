package dev.szx.dimensionworks.migration;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/** 实际玩家 UUID/名称的内存 NBT 读取；FakePlayer 不代表网络登录验收。 */
@Mod("dimensionworks_migration_player_batch")
public final class NativePlayerBatch {
    public NativePlayerBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated player fixture");
            Path output = cwd.resolve("local/migration-player-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse previous player output");
            CompoundTag input = NbtIo.readCompressed(cwd.resolve("local/migration-player-input.nbt"), NbtAccounter.unlimitedHeap());
            if (input.getInt("format") != 1) throw new IllegalStateException("Wrong player batch format");
            var registry = event.getServer().registryAccess();
            ListTag rows = new ListTag(); Set<UUID> uuids = new HashSet<>();
            for (Tag value : input.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value, data = row.getCompound("data");
                try {
                    UUID uuid = data.getUUID("UUID");
                    if (!uuids.add(uuid)) throw new IllegalStateException("Duplicate actual player UUID");
                    ServerLevel level = event.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.getString("Dimension"))));
                    if (level == null) throw new IllegalStateException("Actual player dimension is absent from target fixture");
                    GameProfile profile = new GameProfile(uuid, row.getString("name"));
                    FakePlayer player = new FakePlayer(level, profile);
                    initializeCurios(player, data);
                    player.load(data.copy());
                    CompoundTag saved = player.saveWithoutId(new CompoundTag());
                    if (!player.getUUID().equals(uuid) || !saved.getUUID("UUID").equals(uuid))
                        throw new IllegalStateException("Native player UUID changed");
                    CompoundTag first = inventory(player);
                    FakePlayer second = new FakePlayer(level, profile);
                    initializeCurios(second, data);
                    second.load(saved.copy());
                    CompoundTag reread = second.saveWithoutId(new CompoundTag()), secondInventory = inventory(second);
                    if (!first.equals(secondInventory)) throw new IllegalStateException("Player inventory/ender/curios changed on reload");
                    CompoundTag converted = new CompoundTag(); converted.putString("label", row.getString("label"));
                    converted.putString("name", profile.getName()); converted.putUUID("uuid", uuid);
                    converted.put("data", saved); converted.put("reloaded_data", reread); converted.put("inventory", first);
                    converted.putBoolean("inventory_reload_stable", true); converted.putBoolean("network_login_verified", false);
                    rows.add(converted);
                } catch (Exception error) { throw new IllegalStateException("Native player failed at " + row.getString("label"), error); }
            }
            CompoundTag result = new CompoundTag(); result.putInt("format", 1); result.put("rows", rows);
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(result, output);
            System.out.println("MIGRATION_PLAYER_BATCH_DONE rows=" + rows.size());
        } catch (Exception error) { throw new RuntimeException("Native player serialization batch failed", error); }
    }

    private static void initializeCurios(FakePlayer player, CompoundTag source) {
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(() -> new IllegalStateException("Player Curios inventory is absent"));
        inventory.reset(); // 在载入前初始化实际目标槽位；不能在读取旧库存后 reset。
        for (Tag value : source.getCompound("neoforge:attachments").getCompound("curios:inventory").getList("Curios", Tag.TAG_COMPOUND)) {
            String identifier = ((CompoundTag) value).getString("Identifier");
            if (!inventory.getCurios().containsKey(identifier)) throw new IllegalStateException("Missing target Curios slot " + identifier);
        }
    }

    private static CompoundTag inventory(FakePlayer player) {
        CompoundTag result = new CompoundTag();
        result.put("Inventory", player.getInventory().save(new ListTag()));
        result.put("EnderItems", player.getEnderChestInventory().createTag(player.registryAccess()));
        CompoundTag curios = new CompoundTag();
        CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().forEach((key, handler) -> curios.put(key, handler.serializeNBT()));
        result.put("Curios", curios);
        return result;
    }
}
