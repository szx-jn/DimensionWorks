package dev.szx.dimensionworks.migration;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** 全部实际方块实体的原生持久化批次；只创建独立对象，不放置方块或启动真实机器。 */
@Mod("dimensionworks_migration_be_batch")
public final class NativeBlockEntityBatch {
    private MinecraftServer pending;
    public NativeBlockEntityBatch(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(this::started); NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void started(ServerStartedEvent event) {
        pending = event.getServer();
    }

    private void tick(ServerTickEvent.Post event) {
        if (pending != null) { MinecraftServer server = pending; pending = null; runBatch(server); }
    }

    private void runBatch(MinecraftServer server) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated block entity fixture");
            Path output = cwd.resolve("local/migration-be-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse previous native block entity output");
            CompoundTag input = NbtIo.readCompressed(cwd.resolve("local/migration-be-input.nbt"), NbtAccounter.unlimitedHeap());
            String phase = input.getString("diagnostic_id");
            if (input.getInt("format") != 1 || !phase.matches("[a-z0-9-]{1,64}"))
                throw new IllegalStateException("Invalid private block entity phase");
            Path diagnostics = cwd.resolve("local/migration-be-errors-" + phase + ".log");
            privateFile(diagnostics);
            var registry = server.registryAccess();
            var ops = RegistryOps.create(NbtOps.INSTANCE, registry);
            ListTag rows = new ListTag(), errors = new ListTag();
            try (var log = new PrintWriter(Files.newBufferedWriter(diagnostics))) {
                for (Tag value : input.getList("rows", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) value;
                    try {
                        CompoundTag data = row.getCompound("data");
                        BlockState state = BlockState.CODEC.parse(ops, row.getCompound("state")).getOrThrow();
                        BlockPos pos = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                            ResourceLocation.parse(row.getString("dimension"))));
                        if (level == null) throw new IllegalStateException("Actual source dimension is absent from target");
                        BlockEntity tile = create(state, pos, data.getString("id"));
                        // 原版刷怪笼 load 会查询未加载坐标并通知世界；纯 NBT 对象不需要世界。
                        if (!data.getString("id").equals("minecraft:mob_spawner")) tile.setLevel(level);
                        tile.loadWithComponents(data.copy(), registry);
                        CompoundTag saved = tile.saveWithFullMetadata(registry);
                        if (saved.getInt("x") != pos.getX() || saved.getInt("y") != pos.getY() || saved.getInt("z") != pos.getZ())
                            throw new IllegalStateException("Native block entity position changed");
                        BlockEntity second = create(state, pos, saved.getString("id"));
                        if (!saved.getString("id").equals("minecraft:mob_spawner")) second.setLevel(level);
                        second.loadWithComponents(saved.copy(), registry);
                        CompoundTag reloaded = second.saveWithFullMetadata(registry);
                        boolean stable = saved.equals(reloaded);
                        boolean derivedRpm = !stable && input.getBoolean("detached_kinetic_objects") &&
                            tile instanceof KineticBlockEntity && equalExceptDetachedRpmCaches(saved, reloaded);
                        if (!stable && !derivedRpm) {
                            log.println("UNSTABLE " + row.getString("label")); log.flush();
                            CompoundTag failed = new CompoundTag(); failed.putString("label", row.getString("label"));
                            failed.putString("error", "Native block entity did not stabilize on reload"); errors.add(failed);
                        }
                        CompoundTag converted = new CompoundTag(); converted.putString("label", row.getString("label"));
                        converted.put("data", saved); converted.put("reloaded_data", reloaded);
                        converted.putBoolean("reload_stable", stable);
                        converted.putBoolean("detached_rpm_cache_changed", derivedRpm);
                        converted.putBoolean("reload_state_equal", stable || derivedRpm); rows.add(converted);
                    } catch (Exception error) {
                        log.println("ROW " + row.getString("label")); error.printStackTrace(log); log.flush();
                        CompoundTag failed = new CompoundTag(); failed.putString("label", row.getString("label"));
                        failed.putString("error", error.toString()); errors.add(failed);
                        if (!input.getBoolean("collect_errors")) throw error;
                    }
                    if ((rows.size() + errors.size()) % 2000 == 0)
                        System.out.println("MIGRATION_BE_BATCH_PROGRESS ready=" + rows.size() + " errors=" + errors.size());
                }
            }
            CompoundTag result = new CompoundTag(); result.putInt("format", 1); result.put("rows", rows); result.put("errors", errors);
            result.putBoolean("complete", errors.isEmpty()); privateFile(output); NbtIo.writeCompressed(result, output);
            if (!errors.isEmpty()) throw new IllegalStateException("Native block entity batch has " + errors.size() + " errors; see private diagnostics");
            System.out.println("MIGRATION_BE_BATCH_DONE rows=" + rows.size());
        } catch (Exception error) { throw new RuntimeException("Native block entity persistence batch failed", error); }
    }

    // 独立对象没有真实动力网络；只允许这两个派生缓存重算，所有其它持久字段必须完全相同。
    // 保存的两份原始结果均保留供对账，不用这里的离网缓存覆盖转换后的真实世界。
    private static boolean equalExceptDetachedRpmCaches(CompoundTag first, CompoundTag second) {
        CompoundTag a = first.copy(), b = second.copy();
        for (String key : new String[]{"DWNetworkRpmLimit", "dimensionworks_machine_rpm_limit"}) {
            CompoundTag x = a.getCompound("NeoForgeData"), y = b.getCompound("NeoForgeData");
            if (!java.util.Objects.equals(x.get(key), y.get(key))) {
                if (!x.contains(key, Tag.TAG_INT) || !y.contains(key, Tag.TAG_INT) ||
                    x.getInt(key) < 0 || x.getInt(key) > 10240 || y.getInt(key) < 0 || y.getInt(key) > 10240)
                    return false;
                x.remove(key); y.remove(key);
            }
        }
        return a.equals(b);
    }

    private static BlockEntity create(BlockState state, BlockPos pos, String expectedId) {
        if (!(state.getBlock() instanceof EntityBlock block)) throw new IllegalStateException("Actual state has no native block entity");
        BlockEntity tile = block.newBlockEntity(pos, state);
        if (tile == null) throw new IllegalStateException("Actual state did not construct a block entity");
        String actualId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType()).toString();
        if (!actualId.equals(expectedId))
            throw new IllegalStateException("Actual state entity type " + actualId + " disagrees with saved " + expectedId);
        return tile;
    }

    private static void privateFile(Path path) throws java.io.IOException {
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }
}
