package dev.szx.dimensionworks.spectatortoggle.handler;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player lifecycle handling and cleanup before the server saves players on shutdown. */
@Mod.EventBusSubscriber(modid = DimensionWorksSpectatorToggle.MOD_ID)
public final class SpectatorToggleEvents {
    private static final String RETURN_POINT_KEY = SpectatorToggleState.RETURN_POINT_KEY;

    private SpectatorToggleEvents() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SpectatorToggleController.resetOnServerStopping(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpectatorToggleController.resetExpiredState(player);
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
