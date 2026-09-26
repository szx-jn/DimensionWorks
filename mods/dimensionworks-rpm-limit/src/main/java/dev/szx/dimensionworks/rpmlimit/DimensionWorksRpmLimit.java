package dev.szx.dimensionworks.rpmlimit;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DimensionWorksRpmLimit.MOD_ID)
public final class DimensionWorksRpmLimit {

    public static final String MOD_ID = "dimensionworks_rpm_limit";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DimensionWorksRpmLimit() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RpmLimitConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(RpmForgeEvents.class);

        // The overspeed step table is parsed from a config string rather than read per machine, so a
        // reload has to swap the parsed table over instead of re-parsing it on every tick.
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener((ModConfigEvent.Loading event) -> onConfig(event));
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener((ModConfigEvent.Reloading event) -> onConfig(event));
    }

    private void onConfig(ModConfigEvent event) {
        RpmForgeEvents.config(event);
        if (event.getConfig().getSpec() == RpmLimitConfig.SPEC)
            OverspeedBonus.reloadCurve();
    }
}
