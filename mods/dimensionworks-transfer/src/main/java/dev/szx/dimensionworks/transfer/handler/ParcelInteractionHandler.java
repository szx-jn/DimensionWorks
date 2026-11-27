package dev.szx.dimensionworks.transfer.handler;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.api.TransferServices;
import dev.szx.dimensionworks.transfer.compat.PackedPlayerInteractions;
import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Handles packed-player damage rules, tag synchronization, and packager right-click unpacking. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class ParcelInteractionHandler {
    private ParcelInteractionHandler() {
    }

    @SubscribeEvent
    public static void synchronizeParcelTag(PlayerTickEvent.Pre event) {
        PackedPlayerTransport.synchronize(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PackedPlayerInteractions.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UnpackFireworks.cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        UnpackFireworks.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        UnpackFireworks.tick(event.getServer());
    }

    @SubscribeEvent
    public static void parcelDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PlayerParcel.isPacked(player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FALL)
            || (event.getSource().is(DamageTypes.IN_WALL) && PackedPlayerTransport.ignoresBlocks(player))) {
            event.setCanceled(true);
            return;
        }
        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && !TransferServices.isTutorialLocked(player)
            && PlayerParcel.unpack(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void usePackager(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
            && player.level().getBlockEntity(event.getPos()) instanceof PackagerBlockEntity packager
            && PackedPlayerInteractions.unpackAt(player, packager)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
