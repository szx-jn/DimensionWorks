package dev.szx.dimensionworks.transfer.territory;

/**
 * One player's 3x3 chunk plot. Plots are laid out on a square grid whose cell size is
 * {@link #STRIDE_CHUNKS} chunks, so two neighbouring plots always leave {@link #MIN_GAP_CHUNKS}
 * chunks of empty space between their edges.
 */
public record Territory(int spiralIndex, int gridX, int gridZ) {
    /** Width and depth of a plot, in chunks. */
    public static final int TERRITORY_CHUNKS = 3;

    /** Empty chunks kept between two neighbouring plots. Must stay above 10. */
    public static final int MIN_GAP_CHUNKS = 11;

    /** Distance between the origin chunks of two neighbouring plots. */
    public static final int STRIDE_CHUNKS = TERRITORY_CHUNKS + MIN_GAP_CHUNKS;

    /** Topmost stone layer; players stand on {@code FLOOR_Y + 1}. */
    public static final int FLOOR_Y = -64;

    /** Bottom layer of the plot, made of bedrock. */
    public static final int BEDROCK_Y = -128;

    public int chunkX() {
        return gridX * STRIDE_CHUNKS;
    }

    public int chunkZ() {
        return gridZ * STRIDE_CHUNKS;
    }

    /** Smallest block X that belongs to this plot (inclusive). */
    public int minX() {
        return chunkX() * 16;
    }

    /** Largest block X that belongs to this plot (inclusive). */
    public int maxX() {
        return minX() + TERRITORY_CHUNKS * 16 - 1;
    }

    /** Smallest block Z that belongs to this plot (inclusive). */
    public int minZ() {
        return chunkZ() * 16;
    }

    /** Largest block Z that belongs to this plot (inclusive). */
    public int maxZ() {
        return minZ() + TERRITORY_CHUNKS * 16 - 1;
    }

    /** First walkable block on the X axis, inside the barrier ring. */
    public int interiorMinX() {
        return minX() + 1;
    }

    /** Last walkable block on the X axis, inside the barrier ring. */
    public int interiorMaxX() {
        return maxX() - 1;
    }

    /** First walkable block on the Z axis, inside the barrier ring. */
    public int interiorMinZ() {
        return minZ() + 1;
    }

    /** Last walkable block on the Z axis, inside the barrier ring. */
    public int interiorMaxZ() {
        return maxZ() - 1;
    }

    public double centerX() {
        return minX() + (TERRITORY_CHUNKS * 16) / 2.0D;
    }

    public double centerZ() {
        return minZ() + (TERRITORY_CHUNKS * 16) / 2.0D;
    }

    /** Guard used by every write so nothing can ever be placed outside the plot. */
    public boolean contains(int x, int z) {
        return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ();
    }
}
