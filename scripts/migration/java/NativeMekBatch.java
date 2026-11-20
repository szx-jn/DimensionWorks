package dev.szx.dimensionworks.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.security.SecurityMode;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.lib.security.SecurityFrequency;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.IRedstoneControl;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** Temporary native serialization bridge. Only reads supplied NBT; never loads the old world. */
@Mod("dimensionworks_migration_mek_batch")
public final class NativeMekBatch {
    public NativeMekBatch(IEventBus bus) { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            if (!Files.readString(cwd.resolve("dimensionworks-pack-check.marker")).strip().equals("isolated-pack-check"))
                throw new IllegalStateException("Refuse non-isolated server");
            Path output = cwd.resolve("local/migration-mek-output.nbt");
            if (Files.exists(output)) throw new IllegalStateException("Refuse existing output");
            var source = NbtIo.readCompressed(cwd.resolve("local/migration-mek-input.nbt"), NbtAccounter.unlimitedHeap());
            if (source.getInt("format") != 1) throw new IllegalStateException("Wrong input format");
            var registry = event.getServer().registryAccess();
            var ops = RegistryOps.create(NbtOps.INSTANCE, registry);
            CompoundTag result = new CompoundTag(); result.putInt("format", 1);
            if (source.getString("mode").equals("schema")) {
                CompoundTag items = new CompoundTag(), blocks = new CompoundTag(), enums = new CompoundTag();
                for (Tag value : source.getList("item_ids", Tag.TAG_STRING)) {
                    String id = value.getAsString(); var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
                    if (stack.isEmpty()) throw new IllegalStateException("Unknown item " + id);
                    items.put(id, DataComponentMap.CODEC.encodeStart(ops, stack.getComponents()).getOrThrow());
                }
                for (Tag value : source.getList("block_ids", Tag.TAG_STRING)) {
                    String id = value.getAsString(); BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
                    BlockEntity tile = tile(state, BlockPos.ZERO); tile.setLevel(event.getServer().overworld());
                    CompoundTag row = new CompoundTag(); row.put("saved", tile.saveWithFullMetadata(registry));
                    row.put("components", DataComponentMap.CODEC.encodeStart(ops, tile.collectComponents()).getOrThrow());
                    row.put("state", BlockState.CODEC.encodeStart(ops, state).getOrThrow()); blocks.put(id, row);
                }
                enums.put("side", enums(RelativeSide.values())); enums.put("data_type", enums(DataType.values()));
                enums.put("transmission", enums(TransmissionType.values())); enums.put("upgrade", enums(Upgrade.values()));
                enums.put("configurator", enums(ItemConfigurator.ConfiguratorMode.values()));
                enums.put("redstone", enums(IRedstoneControl.RedstoneControl.values())); enums.put("security", enums(SecurityMode.values()));
                result.put("items", items); result.put("blocks", blocks); result.put("enums", enums);
            } else if (source.getString("mode").equals("convert")) {
                ListTag rows = new ListTag();
                for (Tag value : source.getList("rows", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) value, converted = new CompoundTag();
                    converted.putString("label", row.getString("label"));
                    if (row.getString("kind").equals("block_entity")) {
                        CompoundTag data = row.getCompound("data");
                        BlockPos pos = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                        BlockState state = BlockState.CODEC.parse(ops, row.getCompound("state")).getOrThrow();
                        BlockEntity block = tile(state, pos); block.setLevel(event.getServer().overworld()); block.loadWithComponents(data, registry);
                        CompoundTag saved = block.saveWithFullMetadata(registry);
                        BlockEntity reloaded = tile(state, pos); reloaded.setLevel(event.getServer().overworld()); reloaded.loadWithComponents(saved, registry);
                        if (!saved.equals(reloaded.saveWithFullMetadata(registry))) throw new IllegalStateException("Native BE reload changed data: " + row.getString("label"));
                        converted.put("data", saved); converted.put("components", DataComponentMap.CODEC.encodeStart(ops, block.collectComponents()).getOrThrow());
                    } else if (row.getString("kind").equals("item")) {
                        ItemStack stack = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(ops, row.getCompound("stack")).getOrThrow();
                        if (row.contains("tile_data", Tag.TAG_COMPOUND)) {
                            if (!(stack.getItem() instanceof BlockItem item)) throw new IllegalStateException("Tile data on non-block item");
                            BlockEntity block = tile(item.getBlock().defaultBlockState(), BlockPos.ZERO);
                            block.setLevel(event.getServer().overworld()); block.loadWithComponents(row.getCompound("tile_data"), registry);
                            stack.applyComponents(block.collectComponents());
                            var custom = stack.get(DataComponents.CUSTOM_DATA);
                            if (custom == null || !custom.copyTag().contains("mekData", Tag.TAG_COMPOUND)) throw new IllegalStateException("Missing source Mek custom data");
                            CompoundTag remaining = custom.copyTag(); remaining.remove("mekData");
                            if (remaining.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
                            else stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(remaining));
                        }
                        Tag encoded = CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encodeStart(ops, stack).getOrThrow();
                        rejectMissing(encoded); converted.put("stack", encoded);
                    } else if (row.getString("kind").equals("security_frequency")) {
                        var frequency = SecurityFrequency.CODEC.parse(ops, row.getCompound("data")).getOrThrow();
                        converted.put("data", SecurityFrequency.CODEC.encodeStart(ops, frequency).getOrThrow());
                    } else if (row.getString("kind").equals("evaporation_cache")) {
                        MultiblockCache<?> cache = new MultiblockCache<>();
                        cache.load(registry, row.getCompound("data"));
                        CompoundTag data = new CompoundTag(); cache.save(registry, data);
                        data.put("inventory_id", row.getCompound("data").get("inventory_id").copy());
                        converted.put("data", data);
                    } else throw new IllegalStateException("Unknown row kind");
                    rows.add(converted);
                }
                result.put("rows", rows);
            } else throw new IllegalStateException("Unknown input mode");
            Files.createFile(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            NbtIo.writeCompressed(result, output);
            System.out.println("MIGRATION_MEK_BATCH_DONE mode=" + source.getString("mode") + " rows=" + result.getList("rows", Tag.TAG_COMPOUND).size());
        } catch (Exception error) { throw new RuntimeException("Native Mek serialization batch failed", error); }
    }

    private static BlockEntity tile(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof EntityBlock block)) throw new IllegalStateException("No BE for block " + state);
        BlockEntity tile = block.newBlockEntity(pos, state);
        if (tile == null) throw new IllegalStateException("Missing BE constructor");
        return tile;
    }

    private static ListTag enums(Enum<?>[] values) {
        ListTag list = new ListTag();
        for (Enum<?> value : values) {
            if (!(value instanceof StringRepresentable name)) throw new IllegalStateException("No persistent enum name");
            CompoundTag row = new CompoundTag(); row.putInt("ordinal", value.ordinal());
            row.putString("name", value.name()); row.putString("serialized", name.getSerializedName()); list.add(row);
        }
        return list;
    }

    private static void rejectMissing(Tag value) {
        if (value instanceof CompoundTag c) {
            if (c.getCompound("components").contains("ae2:missing_content_error")) throw new IllegalStateException("Missing nested AE content");
            for (String key : c.getAllKeys()) rejectMissing(c.get(key));
        } else if (value instanceof ListTag list) for (Tag child : list) rejectMissing(child);
    }
}
