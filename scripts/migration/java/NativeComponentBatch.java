package dev.szx.dimensionworks.migration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LegacyBackpackDataMigration;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.LegacyUpgradeDataMigration;
import net.p3pp3rf1y.sophisticatedstorage.item.LegacyStorageBlockDataMigration;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
            ListTag results = new ListTag(), failures = new ListTag();
            List<RuntimeException> errors = new ArrayList<>();
            String diagnosticId = source.getString("diagnostic_id");
            if (source.getBoolean("collect_errors") && !diagnosticId.matches("[a-z0-9-]{1,50}"))
                throw new IllegalStateException("Missing private diagnostic id");
            for (Tag value : source.getList("rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value;
                try {
                    results.add(convertRow(row, registry, ops));
                } catch (RuntimeException error) {
                    if (!source.getBoolean("collect_errors")) throw error;
                    errors.add(error);
                    CompoundTag failure = new CompoundTag();
                    failure.putString("label", row.getString("label"));
                    Throwable cause = error;
                    while (cause.getCause() != null) cause = cause.getCause();
                    String message = cause.toString();
                    failure.putString("cause", message.substring(0, Math.min(message.length(), 4096)));
                    failures.add(failure);
                }
            }
            CompoundTag report = new CompoundTag(); report.putInt("format", 1); report.put("rows", results);
            report.putBoolean("complete", failures.isEmpty()); report.put("errors", failures);
            if (source.contains("default_item_ids", Tag.TAG_LIST)) {
                CompoundTag defaults = new CompoundTag();
                for (Tag value : source.getList("default_item_ids", Tag.TAG_STRING)) {
                    String id = value.getAsString();
                    if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))
                        throw new IllegalStateException("Unknown default item");
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
                    defaults.put(id, DataComponentMap.CODEC.encodeStart(ops, stack.getComponents()).getOrThrow());
                }
                report.put("default_items", defaults);
            }
            if (source.getBoolean("export_block_state_schema")) {
                CompoundTag blocks = new CompoundTag();
                for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
                    CompoundTag row = new CompoundTag(), properties = new CompoundTag();
                    for (Property<?> property : entry.getValue().getStateDefinition().getProperties())
                        properties.put(property.getName(), propertyValues(property));
                    row.put("properties", properties);
                    row.put("default_state", BlockState.CODEC.encodeStart(ops, entry.getValue().defaultBlockState()).getOrThrow());
                    blocks.put(entry.getKey().location().toString(), row);
                }
                report.put("block_state_schema", blocks);
            }
            if (source.getBoolean("export_potion_effects")) {
                CompoundTag potions = new CompoundTag();
                for (var entry : BuiltInRegistries.POTION.entrySet()) {
                    ListTag effects = new ListTag();
                    for (MobEffectInstance effect : entry.getValue().getEffects())
                        effects.add(MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow());
                    potions.put(entry.getKey().location().toString(), effects);
                }
                report.put("potion_effects", potions);
            }
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(report, output);
            if (!errors.isEmpty()) {
                Path diagnostics = cwd.resolve("local/migration-component-errors-" + diagnosticId + ".log");
                Files.createFile(diagnostics, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(diagnostics))) {
                    for (RuntimeException error : errors) error.printStackTrace(writer);
                }
                throw new IllegalStateException("Incomplete native batch: " + failures.size() + " errors; private diagnostics saved");
            }
            System.out.println("MIGRATION_COMPONENT_BATCH_DONE rows=" + results.size());
        } catch (Exception error) {
            throw new RuntimeException("Native migration component batch failed", error);
        }
    }

    private static CompoundTag convertRow(CompoundTag row, RegistryAccess registry, RegistryOps<Tag> ops) {
        try {
            ItemStack stack = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, row.getCompound("stack")).getOrThrow();
            if (row.getBoolean("normalize_sophisticated")) {
                switch (row.getString("item_kind")) {
                    case "backpack" -> LegacyBackpackDataMigration.normalizeLegacyData(stack);
                    case "storage" -> LegacyStorageBlockDataMigration.normalizeLegacyData(stack);
                    case "upgrade" -> LegacyUpgradeDataMigration.migrate(stack, registry);
                    default -> throw new IllegalStateException("Unknown Sophisticated normalization kind");
                }
            }
            Tag encoded = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, stack).getOrThrow();
            rejectMissingContent(encoded);
            ItemStack reread = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, encoded).getOrThrow();
            Tag reencoded = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, reread).getOrThrow();
            Tag effectiveBefore = DataComponentMap.CODEC.encodeStart(ops, stack.getComponents()).getOrThrow();
            Tag effectiveAfter = DataComponentMap.CODEC.encodeStart(ops, reread.getComponents()).getOrThrow();
            // 部分作者组件含 ItemStack，Java equals 比较对象身份；持久数据须按完整原生编码对账。
            if (stack.getItem() != reread.getItem() || stack.getCount() != reread.getCount() ||
                    !encoded.equals(reencoded) || !effectiveBefore.equals(effectiveAfter))
                throw new IllegalStateException("Native item codec changed effective components or quantity");
            CompoundTag result = new CompoundTag(); result.putString("label", row.getString("label"));
            result.putBoolean("codec_roundtrip_stable", true); result.put("stack", encoded);
            result.putBoolean("component_objects_equal", ItemStack.isSameItemSameComponents(stack, reread));
            var remaining = stack.get(DataComponents.CUSTOM_DATA);
            if (remaining != null) result.put("remaining_custom_data", remaining.copyTag());
            return result;
        } catch (RuntimeException error) {
            throw new IllegalStateException("Native item conversion failed at " + row.getString("label"), error);
        }
    }

    private static <T extends Comparable<T>> ListTag propertyValues(Property<T> property) {
        ListTag values = new ListTag();
        for (T value : property.getPossibleValues()) values.add(StringTag.valueOf(property.getName(value)));
        return values;
    }

    private static void rejectMissingContent(Tag value) {
        if (value instanceof CompoundTag compound) {
            if (compound.getCompound("components").contains("ae2:missing_content_error"))
                throw new IllegalStateException("AE2 codec returned a missing-content placeholder");
            for (String key : compound.getAllKeys()) rejectMissingContent(compound.get(key));
        } else if (value instanceof ListTag list) for (Tag child : list) rejectMissingContent(child);
    }
}
