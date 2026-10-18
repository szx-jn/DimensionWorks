package dev.szx.dimensionworks.versiongate;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.versiongate.network.VersionGateNetwork;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DimensionWorksVersionGate.MOD_ID)
public final class DimensionWorksVersionGate {
    public static final String MOD_ID = "dimensionworks_version_gate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DimensionWorksVersionGate() {
        VersionGateNetwork.register();
        LOGGER.info("DimensionWorks Version Gate loaded");
    }
}
