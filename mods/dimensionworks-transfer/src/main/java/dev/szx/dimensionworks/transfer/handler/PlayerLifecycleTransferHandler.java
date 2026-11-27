package dev.szx.dimensionworks.transfer.handler;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.territory.PlotChunkLoading;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Handles player login/logout/respawn and territory entry/exit. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class PlayerLifecycleTransferHandler {
    private PlayerLifecycleTransferHandler() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.inventoryMenu.sendAllDataToRemote();
        if (!event.getTo().equals(WDKeys.TRANSFER_LEVEL)) {
            PlotChunkLoading.deactivate(player.getUUID());
            return;
        }
        TerritoryManager.enter(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlotChunkLoading.deactivate(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            TerritoryManager.enter(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            TerritoryManager.enter(player);
        }
    }
}
