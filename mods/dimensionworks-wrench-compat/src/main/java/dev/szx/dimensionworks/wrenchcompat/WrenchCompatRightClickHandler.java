package dev.szx.dimensionworks.wrenchcompat;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.ftb.mods.ftbultimine.FTBUltimine;
import dev.ftb.mods.ftbultimine.FTBUltiminePlayerData;
import dev.ftb.mods.ftbultimine.api.rightclick.RightClickHandler;
import dev.ftb.mods.ftbultimine.shape.ShapeContext;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Replays the server-side block interaction that FTB's generic right-click path misses.
 *
 * <p>Create's own wrenchable blocks are already handled by Create Ultimine's mixin. This
 * handler only runs after that mixin reports no work, and it deliberately targets
 * event-based AE2 blocks and Mekanism's {@code Block#use} path.</p>
 */
public final class WrenchCompatRightClickHandler implements RightClickHandler {
    public static final WrenchCompatRightClickHandler INSTANCE = new WrenchCompatRightClickHandler();

    private static final TagKey<Item> FORGE_WRENCHES =
        ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/wrench"));
    private static final ThreadLocal<Boolean> REPLAYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WrenchCompatRightClickHandler() {
    }

    @Override
    @SuppressWarnings("deprecation")
    public int handleRightClickBlock(
        ShapeContext context,
        InteractionHand hand,
        Collection<BlockPos> positions
    ) {
        if (REPLAYING.get()) {
            return 0;
        }
        ServerPlayer player = context.player();
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return 0;
        }

        ItemStack wrench = player.getItemInHand(hand);
        if (!(wrench.getItem() instanceof WrenchItem) || !wrench.is(FORGE_WRENCHES)) {
            return 0;
        }

        Level level = player.level();
        BlockHitResult tracedHit = traceHit(player);
        FTBUltiminePlayerData playerData = getPlayerData(player);
        boolean suspendUltimine = playerData != null && playerData.isPressed();

        if (suspendUltimine) {
            // The synthetic event must not re-enter FTB's right-click dispatcher.
            playerData.setPressed(false);
        }

        REPLAYING.set(true);
        try {
            int didWork = 0;
            for (BlockPos candidate : positions) {
                BlockPos pos = candidate.immutable();
                if (!level.hasChunkAt(pos)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }

                BlockHitResult hitResult = hitAt(tracedHit, pos, context.face());

                boolean ae2Like = isAe2Like(state, level, pos);
                boolean mekanismLike = isMekanismLike(state, level, pos);
                if (!ae2Like && !mekanismLike) {
                    continue;
                }

                PlayerInteractEvent.RightClickBlock event =
                    new PlayerInteractEvent.RightClickBlock(player, hand, pos, hitResult);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    // AE2 and other event-based wrenches set a consuming result when they did work.
                    InteractionResult cancellationResult = event.getCancellationResult();
                    if (cancellationResult != null && cancellationResult.consumesAction()) {
                        didWork++;
                    }
                    continue;
                }

                if (!mekanismLike) {
                    continue;
                }

                BlockState currentState = level.getBlockState(pos);
                if (currentState.isAir()) {
                    continue;
                }
                InteractionResult result = currentState.use(level, player, hand, hitResult);
                if (result.consumesAction()) {
                    didWork++;
                }
            }
            return didWork;
        } finally {
            REPLAYING.remove();
            if (suspendUltimine) {
                playerData.setPressed(true);
            }
        }
    }

    private static FTBUltiminePlayerData getPlayerData(ServerPlayer player) {
        return FTBUltimine.instance == null ? null : FTBUltimine.instance.getOrCreatePlayerData(player);
    }

    private static BlockHitResult traceHit(ServerPlayer player) {
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        HitResult hit = player.pick(reach, 1.0F, false);
        return hit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    private static BlockHitResult hitAt(BlockHitResult tracedHit, BlockPos pos, Direction fallbackFace) {
        if (tracedHit == null) {
            Direction face = fallbackFace == null ? Direction.UP : fallbackFace;
            return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5D), face, pos, false);
        }

        Vec3 local = tracedHit.getLocation().subtract(Vec3.atLowerCornerOf(tracedHit.getBlockPos()));
        Vec3 location = Vec3.atLowerCornerOf(pos).add(local);
        return new BlockHitResult(location, tracedHit.getDirection(), pos, tracedHit.isInside());
    }

    private static boolean isAe2Like(BlockState state, Level level, BlockPos pos) {
        if (hasClassPrefix(state.getBlock().getClass(), "appeng.")) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && hasClassPrefix(blockEntity.getClass(), "appeng.")) {
            return true;
        }

        return namespaceMatches(state, "ae2", "appliedenergistics");
    }

    private static boolean isMekanismLike(BlockState state, Level level, BlockPos pos) {
        if (hasClassPrefix(state.getBlock().getClass(), "mekanism.")) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && hasClassPrefix(blockEntity.getClass(), "mekanism.")) {
            return true;
        }

        return namespaceContains(state, "mekanism");
    }

    private static boolean hasClassPrefix(Class<?> type, String prefix) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current.getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean namespaceMatches(BlockState state, String... prefixes) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }

        String namespace = id.getNamespace();
        for (String prefix : prefixes) {
            if (namespace.equals(prefix) || namespace.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean namespaceContains(BlockState state, String value) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().contains(value);
    }
}
