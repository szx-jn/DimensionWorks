package dev.szx.dimensionworks.transfer.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpiralAllocatorTest {
    private static final int CLAIMS = 2000;

    private static final int[][] RING_ONE = {
            {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}
    };

    @Test
    void firstClaimSitsOnTheGridOrigin() {
        assertGrid(0, 0, 0);
    }

    @Test
    void firstRingIsWalkedInOrder() {
        for (int i = 0; i < RING_ONE.length; i++) {
            assertGrid(i + 1, RING_ONE[i][0], RING_ONE[i][1]);
        }
    }

    /** Guards the ring-boundary bug: the last cell of one ring must not repeat in the next index. */
    @Test
    void everyCellIsClaimedExactlyOnceOnTheCorrectRing() {
        Set<Long> seen = new HashSet<>();
        for (int index = 0; index < CLAIMS; index++) {
            int[] grid = SpiralAllocator.indexToGrid(index);
            long key = ((long) grid[0] << 32) ^ (grid[1] & 0xFFFFFFFFL);
            assertTrue(seen.add(key),
                    "cell " + grid[0] + "," + grid[1] + " was handed out twice at index " + index);
            assertEquals(ringOf(grid[0], grid[1]), ringOfIndex(index),
                    "index " + index + " landed on the wrong ring");
        }
    }

    /** The requirement is a gap of more than ten chunks, and the layout must never be a long strip. */
    @Test
    void neighbouringPlotsStayMoreThanTenChunksApart() {
        Territory[] plots = new Territory[CLAIMS];
        for (int index = 0; index < CLAIMS; index++) {
            int[] grid = SpiralAllocator.indexToGrid(index);
            plots[index] = new Territory(index, grid[0], grid[1]);
        }

        int closest = Integer.MAX_VALUE;
        int widestRing = 0;
        for (int i = 0; i < CLAIMS; i++) {
            widestRing = Math.max(widestRing, ringOf(plots[i].gridX(), plots[i].gridZ()));
            for (int j = i + 1; j < CLAIMS; j++) {
                closest = Math.min(closest, gapBetween(plots[i], plots[j]));
            }
        }

        assertEquals(Territory.MIN_GAP_CHUNKS, closest);
        assertTrue(closest > 10, "plots must be more than ten chunks apart, was " + closest);
        assertTrue(widestRing < 32, "2000 plots should stay compact, widest ring was " + widestRing);
    }

    @Test
    void ringCapacityMatchesTheSquareSpiral() {
        for (int ring = 0; ring <= 8; ring++) {
            assertEquals((2 * ring + 1) * (2 * ring + 1), SpiralAllocator.capacityOfRing(ring));
        }
    }

    private static void assertGrid(int index, int expectedX, int expectedZ) {
        int[] grid = SpiralAllocator.indexToGrid(index);
        assertEquals(expectedX, grid[0], "gridX at index " + index);
        assertEquals(expectedZ, grid[1], "gridZ at index " + index);
    }

    private static int ringOf(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z));
    }

    private static int ringOfIndex(int index) {
        int ring = 0;
        while ((long) (2 * ring + 1) * (2 * ring + 1) <= index) {
            ring++;
        }
        return ring;
    }

    /** Edge-to-edge gap in chunks, measured as Chebyshev distance between the two footprints. */
    private static int gapBetween(Territory a, Territory b) {
        int gapX = Math.max(a.chunkX() - (b.chunkX() + Territory.TERRITORY_CHUNKS),
                            b.chunkX() - (a.chunkX() + Territory.TERRITORY_CHUNKS));
        int gapZ = Math.max(a.chunkZ() - (b.chunkZ() + Territory.TERRITORY_CHUNKS),
                            b.chunkZ() - (a.chunkZ() + Territory.TERRITORY_CHUNKS));
        return Math.max(gapX, gapZ);
    }
}
