package dev.szx.dimensionworks.versiongate;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.versiongate.network.VersionGateNetwork;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

@Mod(DimensionWorksVersionGate.MOD_ID)
public final class DimensionWorksVersionGate {
    public static final String MOD_ID = "dimensionworks_version_gate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DimensionWorksVersionGate(IEventBus modBus, Dist dist) {
        VersionGateNetwork.register(modBus, dist);
        LOGGER.info("DimensionWorks Version Gate loaded");
    }
}
