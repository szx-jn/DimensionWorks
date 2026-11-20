package dev.szx.dimensionworks.transfer.territory;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;

public final class TransferBlockPolicy {
    public static final TagKey<Block> FORBIDDEN = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(WDTransfer.MOD_ID, "forbidden_transfer_blocks"));
    private static volatile Set<String> namespaces = AeBlockNamespaces.PACK_ADDONS;

    private TransferBlockPolicy() {}

    public static void initialize() {
        var dependencies = new HashMap<String, Set<String>>();
        for (var mod : ModList.get().getMods()) {
            dependencies.put(mod.getModId(), mod.getDependencies().stream()
                .filter(dependency -> dependency.getType() == net.neoforged.neoforgespi.language.IModInfo.DependencyType.REQUIRED)
                .map(dependency -> dependency.getModId()).collect(Collectors.toSet()));
        }
        namespaces = AeBlockNamespaces.resolve(dependencies);
    }

    public static boolean inTransfer(Level level) {
        return level.dimension().equals(WDKeys.TRANSFER_LEVEL);
    }

    public static boolean forbidden(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return (id != null && namespaces.contains(id.getNamespace())) || state.is(FORBIDDEN);
    }

    public static boolean deniesWrite(Level level, BlockPos pos, BlockState state) {
        return !level.isClientSide && inTransfer(level) && !level.restoringBlockSnapshots
            && forbidden(state) && !level.getBlockState(pos).is(state.getBlock());
    }

    /** Read Mekanism's saved original block without mutating or consuming the box. */
    public static boolean forbiddenBoxContents(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof mekanism.common.tile.TileEntityCardboardBox tile)) return false;
        var data = tile.components().get(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get());
        return data != null && forbidden(data.blockState());
    }

    public static void notifyDenied(Player player) {
        if (player != null && !player.level().isClientSide) {
            player.displayClientMessage(Component.literal("中转维度禁止放置或解包 AE2 及其附属方块，请在其他维度操作。")
                .withStyle(ChatFormatting.RED), true);
        }
    }
}
