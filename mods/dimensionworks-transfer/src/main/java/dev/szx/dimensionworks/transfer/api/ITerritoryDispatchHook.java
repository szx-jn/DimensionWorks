package dev.szx.dimensionworks.transfer.api;

import dev.szx.dimensionworks.transfer.territory.Territory;
import net.minecraft.server.level.ServerLevel;

/** Application hook invoked by territory management after claiming/entering a plot. */
@FunctionalInterface
public interface ITerritoryDispatchHook {
    void ensureDispatchTerminal(ServerLevel level, Territory territory);
}
