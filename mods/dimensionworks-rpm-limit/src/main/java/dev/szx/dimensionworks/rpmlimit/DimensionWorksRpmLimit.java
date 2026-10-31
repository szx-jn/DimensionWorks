package dev.szx.dimensionworks.rpmlimit;

import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;

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
        dev.szx.dimensionworks.rpmlimit.controller.AdvancedController.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RpmLimitConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(RpmForgeEvents.class);
        dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork.register();
        MinecraftForge.EVENT_BUS.register(dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork.class);
        MinecraftForge.EVENT_BUS.register(GearHeartForgeEvents.class);
        MinecraftForge.EVENT_BUS.register(GearHeartCommands.class);

        // The overspeed step table is parsed from a config string rather than read per machine, so a
        // reload has to swap the parsed table over instead of re-parsing it on every tick.
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener((ModConfigEvent.Loading event) -> onConfig(event));
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener((ModConfigEvent.Reloading event) -> onConfig(event));
    }

    private void onConfig(ModConfigEvent event) {
        RpmForgeEvents.config(event);
        boolean rpmConfigChanged = event.getConfig().getSpec() == RpmLimitConfig.SPEC;
        boolean createServerConfigChanged =
            "create".equals(event.getConfig().getModId())
                && event.getConfig().getType() == ModConfig.Type.SERVER;

        if (rpmConfigChanged)
            OverspeedBonus.reloadCurve();
        if (rpmConfigChanged || createServerConfigChanged)
            KineticNetworkOptimizer.invalidateAllLoadedNetworks();
    }
}
