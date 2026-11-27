package dev.szx.dimensionworks.transfer.handler;

import net.minecraft.server.level.ServerPlayer;
import dev.szx.dimensionworks.transfer.dispatch.DispatchEventHandler;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Compatibility facade delegating to domain-specific event handlers:
 * {@link PlayerLifecycleTransferHandler}, {@link ParcelInteractionHandler},
 * {@link DispatchEventHandler},
 * {@link FrogportRideHandler}, {@link TerritoryGuardHandler}, and {@code TutorialEvents}.
 */
public final class TransferEvents {
    private TransferEvents() {
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerLifecycleTransferHandler.onChangedDimension(event);
        DispatchEventHandler.onChangedDimension(event);
        FrogportRideHandler.onChangedDimension(event);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerLifecycleTransferHandler.onPlayerLogout(event);
        DispatchEventHandler.onPlayerLogout(event);
        ParcelInteractionHandler.onPlayerLogout(event);
        FrogportRideHandler.onPlayerLogout(event);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerLifecycleTransferHandler.onLogin(event);
        FrogportRideHandler.onPlayerLogin(event);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        PlayerLifecycleTransferHandler.onRespawn(event);
        FrogportRideHandler.onRespawn(event);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        dev.szx.dimensionworks.transfer.tutorial.TutorialEvents.onPlayerTick(event);
        DispatchEventHandler.onPlayerTick(event);
        ParcelInteractionHandler.onPlayerTick(event);
        FrogportRideHandler.onPlayerTick(event);
        TerritoryGuardHandler.onPlayerTick(event);
    }

    static void tickFrogport(ServerPlayer player) {
        FrogportRideHandler.tickFrogport(player);
    }

    public static void parcelDamage(LivingIncomingDamageEvent event) {
        ParcelInteractionHandler.parcelDamage(event);
    }

    public static void blockOldTwilightPortal(EntityTravelToDimensionEvent event) {
        DispatchEventHandler.blockOldTwilightPortal(event);
    }
}
