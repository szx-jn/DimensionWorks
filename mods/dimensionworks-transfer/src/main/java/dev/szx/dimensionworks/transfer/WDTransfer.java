package dev.szx.dimensionworks.transfer;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.transfer.api.ITransferNetworkRouter;
import dev.szx.dimensionworks.transfer.api.TransferServices;
import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import dev.szx.dimensionworks.transfer.worldgen.ModChunkGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
            event.enqueueWork(() -> {
                dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy.initialize();
                TransferServices.registerTutorialLockPolicy(
                    player -> player instanceof ServerPlayer serverPlayer && TutorialManager.locked(serverPlayer));
                TransferServices.registerTerritoryDispatchHook((level, terminal) ->
                    DispatchController.ensure(level, terminal));
                TransferServices.registerNetworkRouter(new ITransferNetworkRouter() {
                    @Override
                    public void onToggleRequested(ServerPlayer player) {
                        if (!TutorialManager.locked(player)) {
                            TransferController.useFromKey(player);
                        }
                    }

                    @Override
                    public void onDispatchChoice(ServerPlayer player, BlockPos pos, String route) {
                        DispatchController.send(player, pos, route);
                    }

                    @Override
                    public void onDispatchClosed(ServerPlayer player) {
                        DispatchController.close(player);
                    }

                    @Override
                    public boolean isAlexCavesUnlocked(ServerPlayer player) {
                        return DispatchController.alexCavesUnlocked(player);
                    }
                });
            }));
        modEventBus.addListener(TransferNetwork::register);
        modEventBus.addListener(dev.szx.dimensionworks.transfer.elastic.ElasticNetwork::register);
        dev.szx.dimensionworks.transfer.elastic.PackagerItems.ITEMS.register(modEventBus);
        dev.szx.dimensionworks.transfer.dispatch.DispatchItems.ITEMS.register(modEventBus);
        DispatchBlocks.BLOCKS.register(modEventBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        LOGGER.info("DimensionWorks Transfer loaded");
    }
}
