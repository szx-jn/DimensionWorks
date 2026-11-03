package dev.szx.dimensionworks.spectatortoggle;

import dev.szx.dimensionworks.spectatortoggle.network.SpectatorToggleNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(DimensionWorksSpectatorToggle.MOD_ID)
public final class DimensionWorksSpectatorToggle {
    public static final String MOD_ID = "dimensionworks_spectator_toggle";

    public DimensionWorksSpectatorToggle() {
        SpectatorToggleNetwork.register();
    }
}
