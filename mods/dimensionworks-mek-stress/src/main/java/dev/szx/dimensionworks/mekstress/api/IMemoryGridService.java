package dev.szx.dimensionworks.mekstress.api;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import net.minecraft.core.GlobalPos;

/** Public read/write contract mounted on one logical AE2 grid. */
public interface IMemoryGridService extends IGridService, IGridServiceProvider {
    MemoryNetworkSnapshot snapshot(long gameTick);

    long insertSu(long amount, Actionable mode, long gameTick);

    long extractSu(long amount, Actionable mode, long gameTick);

    void reportOutputBus(GlobalPos busPos, GlobalPos machinePos, int effectiveRpm, MachineTier tier, long gameTick);

    void reportGearboxExport(GlobalPos gearboxPos, int requestedRpm, long requestedSuPerTick, long gameTick);

    double finalQ(long gameTick);

    void invalidateSnapshot();
}
