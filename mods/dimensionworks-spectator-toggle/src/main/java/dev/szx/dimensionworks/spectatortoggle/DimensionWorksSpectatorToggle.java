package dev.szx.dimensionworks.spectatortoggle;

import dev.szx.dimensionworks.spectatortoggle.network.SpectatorToggleNetwork;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

@Mod(DimensionWorksSpectatorToggle.MOD_ID)
public final class DimensionWorksSpectatorToggle {
    public static final String MOD_ID = "dimensionworks_spectator_toggle";

    public DimensionWorksSpectatorToggle(IEventBus modBus) {
        modBus.addListener(SpectatorToggleNetwork::register);
    }
}
