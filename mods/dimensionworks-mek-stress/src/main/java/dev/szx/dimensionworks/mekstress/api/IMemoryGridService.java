package dev.szx.dimensionworks.mekstress.api;

import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;

/** Public read/write contract mounted on one logical AE2 grid. */
public interface IMemoryGridService extends IGridService, IGridServiceProvider {
    MemoryNetworkSnapshot snapshot(long gameTick);

    double finalQ(long gameTick);

    void invalidateSnapshot();
}
