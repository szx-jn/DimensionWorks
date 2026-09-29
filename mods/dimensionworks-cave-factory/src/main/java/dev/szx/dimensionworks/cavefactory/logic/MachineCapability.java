package dev.szx.dimensionworks.cavefactory.logic;

public enum MachineCapability {
    NONE(false, false, false),
    ITEM_ONLY(true, false, false),
    ITEM_AND_FLUID_TANKS(true, true, false),
    MAPPED_RECIPES(false, false, true),
    ITEM_AND_MAPPED_RECIPES(true, false, true),
    FLUID_AND_MAPPED_RECIPES(false, true, true),
    ITEM_FLUID_AND_MAPPED_RECIPES(true, true, true);

    private final boolean itemHandlers;
    private final boolean fluidTanks;
    private final boolean mappedRecipes;

    MachineCapability(boolean itemHandlers, boolean fluidTanks, boolean mappedRecipes) {
        this.itemHandlers = itemHandlers;
        this.fluidTanks = fluidTanks;
        this.mappedRecipes = mappedRecipes;
    }

    public boolean hasItemHandlers() {
        return itemHandlers;
    }

    public boolean hasFluidTanks() {
        return fluidTanks;
    }

    public boolean hasMappedRecipes() {
        return mappedRecipes;
    }
}
