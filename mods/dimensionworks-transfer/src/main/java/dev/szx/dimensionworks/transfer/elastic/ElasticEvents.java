package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class ElasticEvents {
    private ElasticEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) ElasticParcelState.tick(player);
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
