package dev.szx.dimensionworks.transfer.territory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Writes a plot's blocks in bounded batches so a single claim never stalls the server tick.
 *
 * <p>The walkable floor is placed synchronously, so the player always lands on solid ground.
 * Everything below it, plus the barrier ring, is spread over following ticks.</p>
 */
public final class TerritoryBuilder {
    /** Roughly how many blocks are written per server tick. */
    private static final int BLOCKS_PER_TICK = 4096;

    private static final Deque<Job> JOBS = new ArrayDeque<>();

    private TerritoryBuilder() {
    }

    /** Places the top stone layer immediately so the player has something to stand on. */
    public static void placeFloor(ServerLevel level, Territory territory) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = territory.minX(); x <= territory.maxX(); x++) {
            if (!territory.contains(x, territory.minZ())) {
                continue;
            }
            for (int z = territory.minZ(); z <= territory.maxZ(); z++) {
                if (!territory.contains(x, z)) {
                    continue;
                }
                level.setBlock(new BlockPos(x, Territory.FLOOR_Y, z), stone, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void enqueue(ServerLevel level, Territory territory, UUID playerId) {
        JOBS.add(new Job(level, territory, playerId));
    }

    public static void tick() {
        int budget = BLOCKS_PER_TICK;
        while (budget > 0 && !JOBS.isEmpty()) {
            Job job = JOBS.peek();
            budget -= job.step(budget);
            if (job.isDone()) {
                JOBS.poll();
                job.finish();
            }
        }
    }

    /** Axis-aligned box of one block state, always inside the owning plot. */
    private record Box(int x0, int x1, int y0, int y1, int z0, int z1, BlockState state) {
        long volume() {
            return (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        }

        void place(ServerLevel level, Territory territory, long index) {
            int spanX = x1 - x0 + 1;
            int spanZ = z1 - z0 + 1;
            int x = x0 + (int) (index % spanX);
            int z = z0 + (int) ((index / spanX) % spanZ);
            int y = y0 + (int) (index / ((long) spanX * spanZ));

            if (!territory.contains(x, z)) {
                return;
            }
            level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
        }
    }

    private static final class Job {
        private final ServerLevel level;
        private final Territory territory;
        private final UUID playerId;
        private final List<Box> boxes = new ArrayList<>();
        private int boxIndex;
        private long cursor;

        Job(ServerLevel level, Territory territory, UUID playerId) {
            this.level = level;
            this.territory = territory;
            this.playerId = playerId;

            int x0 = territory.minX();
            int x1 = territory.maxX();
            int z0 = territory.minZ();
            int z1 = territory.maxZ();
            int topY = level.getMaxBuildHeight() - 1;

            BlockState stone = Blocks.STONE.defaultBlockState();
            BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
            BlockState barrier = Blocks.BARRIER.defaultBlockState();

            // The FLOOR_Y layer itself was already written by placeFloor().
            boxes.add(new Box(x0, x1, Territory.BEDROCK_Y + 1, Territory.FLOOR_Y - 1, z0, z1, stone));
            boxes.add(new Box(x0, x1, Territory.BEDROCK_Y, Territory.BEDROCK_Y, z0, z1, bedrock));

            // Barrier ring, hugging the outer blocks of the plot so nothing spills outside it.
            boxes.add(new Box(x0, x0, Territory.BEDROCK_Y, topY, z0, z1, barrier));
            boxes.add(new Box(x1, x1, Territory.BEDROCK_Y, topY, z0, z1, barrier));
            boxes.add(new Box(x0 + 1, x1 - 1, Territory.BEDROCK_Y, topY, z0, z0, barrier));
            boxes.add(new Box(x0 + 1, x1 - 1, Territory.BEDROCK_Y, topY, z1, z1, barrier));

            // Ceiling: the walls already fill the border row at topY, so this closes the interior.
            boxes.add(new Box(x0 + 1, x1 - 1, topY, topY, z0 + 1, z1 - 1, barrier));
        }

        boolean isDone() {
            return boxIndex >= boxes.size();
        }

        int step(int budget) {
            int placed = 0;
            while (placed < budget && boxIndex < boxes.size()) {
                Box box = boxes.get(boxIndex);
                long volume = box.volume();
                while (placed < budget && cursor < volume) {
                    box.place(level, territory, cursor);
                    cursor++;
                    placed++;
                }
                if (cursor >= volume) {
                    boxIndex++;
                    cursor = 0;
                }
            }
            return placed;
        }

        void finish() {
            TerritoryManager.onBuildFinished(level, playerId);
        }
    }
}
