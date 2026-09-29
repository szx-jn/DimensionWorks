package dev.szx.dimensionworks.cavefactory.logic;

public final class StructureLayout {
    public static final int RADIUS = 1;

    private StructureLayout() {}

    public static boolean isWithinCube(int x, int y, int z) {
        return Math.abs(x) <= RADIUS && Math.abs(y) <= RADIUS && Math.abs(z) <= RADIUS;
    }

    public static boolean isCenter(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0;
    }

    public static boolean isFaceCenter(int x, int y, int z) {
        if (!isWithinCube(x, y, z) || isCenter(x, y, z)) {
            return false;
        }
        return Math.abs(x) + Math.abs(y) + Math.abs(z) == 1;
    }

    public static boolean isShellBlock(int x, int y, int z) {
        return isWithinCube(x, y, z) && !isCenter(x, y, z);
    }
}
