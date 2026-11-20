package dev.szx.dimensionworks.migration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LegacyBackpackDataMigration;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.LegacyUpgradeDataMigration;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** 仅放入隔离转换夹具，复用固定目标资产的原生组件及作者迁移接口。 */
@Mod("dimensionworks_migration_component_batch")
public final class NativeComponentBatch {
    public NativeComponentBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated server");
            Path input = cwd.resolve("local/migration-component-input.nbt");
            Path output = cwd.resolve("local/migration-component-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse existing output");
            CompoundTag source = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            if (source.getInt("format") != 1) throw new IllegalStateException("Wrong batch format");
            var registry = event.getServer().registryAccess();
            var ops = RegistryOps.create(NbtOps.INSTANCE, registry);
            ListTag results = new ListTag();
            for (Tag value : source.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value;
                var stack = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, row.getCompound("stack")).getOrThrow();
                if (row.getBoolean("normalize_sophisticated")) {
                    if (row.getString("item_kind").equals("backpack")) LegacyBackpackDataMigration.normalizeLegacyData(stack);
                    else LegacyUpgradeDataMigration.migrate(stack, registry);
                }
                CompoundTag result = new CompoundTag();
                result.putString("label", row.getString("label"));
                result.put("stack", CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, stack).getOrThrow());
                var remaining = stack.get(DataComponents.CUSTOM_DATA);
                if (remaining != null) result.put("remaining_custom_data", remaining.copyTag());
                results.add(result);
            }
            CompoundTag report = new CompoundTag(); report.putInt("format", 1); report.put("rows", results);
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(report, output);
            System.out.println("MIGRATION_COMPONENT_BATCH_DONE rows=" + results.size());
        } catch (Exception error) {
            throw new RuntimeException("Native migration component batch failed", error);
        }
    }
}
