package dev.szx.dimensionworks.transfer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Application hook invoked by territory management after claiming/entering a plot. */
@FunctionalInterface
public interface ITerritoryDispatchHook {
    void ensureDispatchTerminal(ServerLevel level, BlockPos terminal);
}
