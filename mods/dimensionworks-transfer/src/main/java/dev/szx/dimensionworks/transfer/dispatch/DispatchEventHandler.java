package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Owns dispatch UI session cleanup and timeout checks for connected players. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class DispatchEventHandler {
    private DispatchEventHandler() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DispatchController.forceClose(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DispatchController.forget(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DispatchController.tick(player);
        }
    }

    @SubscribeEvent
    public static void blockOldTwilightPortal(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
            && event.getDimension().location().toString().equals("twilightforest:twilight_forest")
            && !DispatchController.authorized(player)
            && !TransferAccess.hasUnlockTag(player)) {
            event.setCanceled(true);
        }
    }
}
