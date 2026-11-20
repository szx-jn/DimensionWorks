package dev.szx.dimensionworks.cavefactory.logic;

/** A successful server datapack reload replaces recipes without replacing RecipeManager. */
public final class FactoryRecipeReload {
    private static long generation;
    private FactoryRecipeReload() {}
    public static long generation() { return generation; }
    public static void invalidate() { generation++; }
}
