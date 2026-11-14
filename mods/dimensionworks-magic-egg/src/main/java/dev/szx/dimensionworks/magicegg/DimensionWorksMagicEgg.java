package dev.szx.dimensionworks.magicegg;

import net.minecraftforge.fml.common.Mod;

@Mod(DimensionWorksMagicEgg.MOD_ID)
public final class DimensionWorksMagicEgg {
    public static final String MOD_ID = "dimensionworks_magic_egg";

    public DimensionWorksMagicEgg() {
        MagicEggNetwork.register();
    }
}
