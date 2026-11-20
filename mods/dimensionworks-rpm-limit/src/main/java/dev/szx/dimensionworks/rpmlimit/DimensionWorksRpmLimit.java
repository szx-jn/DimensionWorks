package dev.szx.dimensionworks.rpmlimit;

import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DimensionWorksRpmLimit.MOD_ID)
public final class DimensionWorksRpmLimit {

    public static final String MOD_ID = "dimensionworks_rpm_limit";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DimensionWorksRpmLimit(IEventBus modBus, ModContainer container, Dist dist) {
        dev.szx.dimensionworks.rpmlimit.controller.AdvancedController.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, RpmLimitConfig.SPEC);
        NeoForge.EVENT_BUS.register(RpmForgeEvents.class);
        dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork.register(modBus, dist);
        NeoForge.EVENT_BUS.register(dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork.class);
        NeoForge.EVENT_BUS.register(GearHeartForgeEvents.class);
        NeoForge.EVENT_BUS.register(GearHeartCommands.class);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GearHeartForgeEvents::registerCuriosItems));

        // The overspeed step table is parsed from a config string rather than read per machine, so a
        // reload has to swap the parsed table over instead of re-parsing it on every tick.
        modBus
            .addListener((ModConfigEvent.Loading event) -> onConfig(event));
        modBus
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
