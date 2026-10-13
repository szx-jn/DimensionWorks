package dev.szx.dimensionworks.rpmlimit.quota;

import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.tile.base.TileEntityMekanism;

import java.util.UUID;

/**
 * Server-thread context for one Mekanism machine tick. The static tickServer entry is wrapped
 * because subclasses override onUpdateServer and a base-class injection would miss them.
 */
public final class MekProcessingContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private MekProcessingContext() {
    }

    public static Context push(TileEntityMekanism machine) {
        Context previous = CURRENT.get();
        UUID owner = GearHeartOwnerResolver.normalize(machine.getOwnerUUID());
        long tick = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
        CURRENT.set(new Context(machine, owner, isOreProcessing(machine), tick));
        return previous;
    }

    public static void pop(Context previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    public static Context current() {
        return CURRENT.get();
    }

    static boolean isOreProcessing(TileEntityMekanism machine) {
        if (!(machine instanceof IRecipeLookupHandler<?> handler)) {
            return false;
        }
        Object recipeType = handler.getRecipeType().getRecipeType();
        return recipeType == MekanismRecipeType.ENRICHING.get()
            || recipeType == MekanismRecipeType.CRUSHING.get()
            || recipeType == MekanismRecipeType.PURIFYING.get()
            || recipeType == MekanismRecipeType.INJECTING.get();
    }

    public record Context(TileEntityMekanism machine, UUID owner,
                          boolean oreProcessing, long tick) {
    }
}
