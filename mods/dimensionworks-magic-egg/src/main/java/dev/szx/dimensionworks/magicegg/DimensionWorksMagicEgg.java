package dev.szx.dimensionworks.magicegg;

import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;

@Mod(DimensionWorksMagicEgg.MOD_ID)
public final class DimensionWorksMagicEgg {
    public static final String MOD_ID = "dimensionworks_magic_egg";

    public DimensionWorksMagicEgg(IEventBus modBus, Dist dist) {
        MagicEggNetwork.register(modBus, dist);
    }
}
