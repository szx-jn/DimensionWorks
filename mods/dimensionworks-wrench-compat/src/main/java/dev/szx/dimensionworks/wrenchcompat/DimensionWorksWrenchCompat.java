package dev.szx.dimensionworks.wrenchcompat;

import dev.ftb.mods.ftbultimine.api.rightclick.RegisterRightClickHandlerEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(DimensionWorksWrenchCompat.MOD_ID)
public final class DimensionWorksWrenchCompat {
    public static final String MOD_ID = "dimensionworks_wrench_compat";

    public DimensionWorksWrenchCompat() {
        RegisterRightClickHandlerEvent.REGISTER.register(
            dispatcher -> dispatcher.registerHandler(WrenchCompatRightClickHandler.INSTANCE)
        );
    }
}
