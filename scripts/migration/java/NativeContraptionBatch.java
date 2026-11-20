package dev.szx.dimensionworks.migration;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.nbt.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** 固定目标 Create 的内存载荷读写；不放置方块、不生成实体、不运行机械。 */
@Mod("dimensionworks_migration_contraption_batch")
public final class NativeContraptionBatch {
    public NativeContraptionBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated fixture");
            Path output = cwd.resolve("local/migration-contraption-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse existing native output");
            CompoundTag source = NbtIo.readCompressed(cwd.resolve("local/migration-contraption-input.nbt"), NbtAccounter.unlimitedHeap());
            if (source.getInt("format") != 1) throw new IllegalStateException("Wrong batch format");
            var registry = event.getServer().registryAccess();
            ListTag rows = new ListTag();
            for (Tag value : source.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value, data = row.getCompound("data");
                try {
                    Contraption first = Contraption.fromNBT(event.getServer().overworld(), data, false);
                    if (first == null || first.getBlocks().size() != data.getCompound("Blocks").getList("BlockList", Tag.TAG_COMPOUND).size())
                        throw new IllegalStateException("Native Create changed block count");
                    CompoundTag written = first.writeNBT(registry, false);
                    // 矿车物品另存这个朝向，基础 Contraption 的 writeNBT 不负责它。
                    if (data.contains("InitialOrientation")) written.put("InitialOrientation", data.get("InitialOrientation").copy());
                    Contraption second = Contraption.fromNBT(event.getServer().overworld(), written, false);
                    if (second == null || second.getBlocks().size() != first.getBlocks().size())
                        throw new IllegalStateException("Native Create reload changed block count");
                    CompoundTag reloaded = second.writeNBT(registry, false);
                    if (written.contains("InitialOrientation")) reloaded.put("InitialOrientation", written.get("InitialOrientation").copy());
                    if (!written.equals(reloaded)) throw new IllegalStateException("Native Create payload did not stabilize on reload");
                    CompoundTag converted = new CompoundTag();
                    converted.putString("label", row.getString("label")); converted.put("data", written);
                    converted.putInt("blocks", first.getBlocks().size()); converted.putBoolean("reload_stable", true);
                    rows.add(converted);
                } catch (RuntimeException error) {
                    throw new IllegalStateException("Native contraption failed at " + row.getString("label"), error);
                }
            }
            CompoundTag result = new CompoundTag(); result.putInt("format", 1); result.put("rows", rows);
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(result, output);
            System.out.println("MIGRATION_CONTRAPTION_BATCH_DONE rows=" + rows.size());
        } catch (Exception error) { throw new RuntimeException("Native contraption batch failed", error); }
    }
}
