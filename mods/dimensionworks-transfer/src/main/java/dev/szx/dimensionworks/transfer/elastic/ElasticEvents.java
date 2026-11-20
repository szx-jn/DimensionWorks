package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class ElasticEvents {
    private ElasticEvents() {}

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) ElasticParcelState.tick(player);
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer player)
            ElasticNetwork.syncTo(player, observer);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ElasticParcelState.synchronize(player);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ElasticParcelState.resetCharge(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) ElasticParcelState.synchronize(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { ElasticParcelState.resetCharge(event.getEntity()); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { ElasticParcelState.resetCharge(event.getEntity()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ElasticParcelState.stop(); }
}
