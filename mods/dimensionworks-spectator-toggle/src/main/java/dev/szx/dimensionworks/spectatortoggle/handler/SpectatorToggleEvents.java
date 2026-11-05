package dev.szx.dimensionworks.spectatortoggle.handler;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps an active return point if a player is recreated by a death respawn. */
@Mod.EventBusSubscriber(modid = DimensionWorksSpectatorToggle.MOD_ID)
public final class SpectatorToggleEvents {
    private static final String RETURN_POINT_KEY = "dimensionworks_spectator_toggle";

    private SpectatorToggleEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SpectatorToggleController.startServerSession();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpectatorToggleController.restoreAfterRestart(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (!oldData.contains(RETURN_POINT_KEY)) {
            return;
        }

        CompoundTag savedPoint = oldData.getCompound(RETURN_POINT_KEY).copy();
        event.getEntity().getPersistentData().put(RETURN_POINT_KEY, savedPoint);
    }
}
