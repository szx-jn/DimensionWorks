package dev.szx.dimensionworks.transfer.territory;

/**
 * Maps a monotonically increasing claim index onto a square spiral in grid space.
 *
 * <p>Ring 0 holds {@code (0,0)}, ring 1 holds the eight cells around it, ring 2 the next
 * sixteen, and so on. The spiral keeps every plot close to the origin and avoids the long
 * single-file layout that would otherwise cause far-from-origin precision problems.</p>
 */
public final class SpiralAllocator {
    private SpiralAllocator() {
    }

    /**
     * @param index zero-based claim index
     * @return grid coordinates {@code [gridX, gridZ]}
     */
    public static int[] indexToGrid(int index) {
        if (index <= 0) {
            return new int[]{0, 0};
        }

        int ring = 1;
        while ((long) (2 * ring + 1) * (2 * ring + 1) <= index) {
            ring++;
        }

        int ringStart = (2 * ring - 1) * (2 * ring - 1);
        int position = index - ringStart;
        int perSide = 2 * ring;
        int side = position / perSide;
        int offset = position % perSide;

        int gridX;
        int gridZ;
        switch (side) {
            case 0 -> {
                gridX = ring;
                gridZ = -ring + offset;
            }
            case 1 -> {
                gridX = ring - offset;
                gridZ = ring;
            }
            case 2 -> {
                gridX = -ring;
                gridZ = ring - offset;
            }
            default -> {
                gridX = -ring + offset;
                gridZ = -ring;
            }
        }
        return new int[]{gridX, gridZ};
    }

    /** Total number of claims that fit inside the given ring (inclusive). */
    public static int capacityOfRing(int ring) {
        int side = 2 * ring + 1;
        return side * side;
    }
}
