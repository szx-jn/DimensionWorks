package dev.szx.dimensionworks.transfer;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import dev.szx.dimensionworks.transfer.worldgen.ModChunkGenerators;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WDTransfer.MOD_ID)
public final class WDTransfer {
    public static final String MOD_ID = "dimensionworks_transfer";

    /** Namespace used by every datapack file that describes the transfer dimension. */
    public static final String DIMENSION_NAMESPACE = "dw";

    public static final Logger LOGGER = LogUtils.getLogger();

    public WDTransfer(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy::initialize));
        TransferNetwork.register();
        dev.szx.dimensionworks.transfer.elastic.ElasticNetwork.register();
        dev.szx.dimensionworks.transfer.elastic.ElasticItems.ITEMS.register(modEventBus);
        dev.szx.dimensionworks.transfer.dispatch.DispatchItems.ITEMS.register(modEventBus);
        DispatchBlocks.BLOCKS.register(modEventBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        LOGGER.info("DimensionWorks Transfer loaded");
    }
}
