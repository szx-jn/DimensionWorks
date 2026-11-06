package dev.szx.dimensionworks.transfer.territory;

import dev.szx.dimensionworks.transfer.WDTransfer;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Checks the standing player's full height even when the traveller is currently packed. */
public final class SafeLanding {
    public static final TagKey<Block> UNSAFE = TagKey.create(Registries.BLOCK,
        new ResourceLocation(WDTransfer.MOD_ID, "unsafe_landing_blocks"));

    private SafeLanding() {}

    public static Optional<BlockPos> inTerritory(ServerLevel level, Territory territory) {
        return search(level, BlockPos.containing(territory.landingX(), Territory.FLOOR_Y + 1,
            territory.landingZ()), 46, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 3,
            pos -> territory.contains(pos.getX(), pos.getZ())
                && pos.getX() >= territory.interiorMinX() && pos.getX() <= territory.interiorMaxX()
                && pos.getZ() >= territory.interiorMinZ() && pos.getZ() <= territory.interiorMaxZ(), false);
    }

    public static Optional<BlockPos> around(ServerLevel level, BlockPos origin, int radius,
                                           int minY, int maxY, Predicate<BlockPos> extra) {
        return search(level, origin, radius, minY, maxY, extra, true);
    }

    private static Optional<BlockPos> search(ServerLevel level, BlockPos origin, int radius,
                                             int minY, int maxY, Predicate<BlockPos> extra,
                                             boolean generate) {
        int preferredY = Mth.clamp(origin.getY(), minY, maxY);
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos column = new BlockPos(origin.getX() + dx, preferredY, origin.getZ() + dz);
                    if (!level.getWorldBorder().isWithinBounds(column) || !extra.test(column)) continue;
                    if (generate) level.getChunk(column);
                    else if (!level.hasChunkAt(column)) continue;
                    for (int delta = 0; delta <= Math.max(preferredY - minY, maxY - preferredY); delta++) {
                        BlockPos up = column.above(delta);
                        if (up.getY() <= maxY && safe(level, up) && extra.test(up)) return Optional.of(up);
                        BlockPos down = column.below(delta);
                        if (delta != 0 && down.getY() >= minY && safe(level, down) && extra.test(down))
                            return Optional.of(down);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean safe(ServerLevel level, BlockPos feet) {
        return safeBlocks(level, feet) && level.noCollision(new AABB(feet.getX() + .2, feet.getY(), feet.getZ() + .2,
            feet.getX() + .8, feet.getY() + 1.8, feet.getZ() + .8));
    }

    public static boolean safeBlocks(BlockGetter level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 2 >= level.getMaxBuildHeight())
            return false;
        BlockState floor = level.getBlockState(feet.below());
        if (!floor.isFaceSturdy(level, feet.below(), Direction.UP) || !floor.getFluidState().isEmpty())
            return false;
        for (int dy = -1; dy <= 2; dy++) {
            BlockPos pos = feet.above(dy);
            BlockState state = level.getBlockState(pos);
            if (dangerous(state)) return false;
            if ((dy == 0 || dy == 1)
                && (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty()))
                return false;
        }
        return true;
    }

    private static boolean dangerous(BlockState state) {
        // Vanilla hazards remain excluded even before the datapack tags have been bound.
        return state.is(UNSAFE) || state.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock
            || state.is(Blocks.LAVA) || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
            || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
    }
}
