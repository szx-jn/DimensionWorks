package dev.szx.dimensionworks.transfer;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import dev.szx.dimensionworks.transfer.worldgen.ModChunkGenerators;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(WDTransfer.MOD_ID)
public final class WDTransfer {
    public static final String MOD_ID = "dimensionworks_transfer";

    /** Namespace used by every datapack file that describes the transfer dimension. */
    public static final String DIMENSION_NAMESPACE = "dw";

    public static final Logger LOGGER = LogUtils.getLogger();

    public WDTransfer(IEventBus modEventBus) {
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy::initialize));
        modEventBus.addListener(TransferNetwork::register);
        modEventBus.addListener(dev.szx.dimensionworks.transfer.elastic.ElasticNetwork::register);
        dev.szx.dimensionworks.transfer.elastic.ElasticItems.ITEMS.register(modEventBus);
        dev.szx.dimensionworks.transfer.dispatch.DispatchItems.ITEMS.register(modEventBus);
        DispatchBlocks.BLOCKS.register(modEventBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        LOGGER.info("DimensionWorks Transfer loaded");
    }
}
