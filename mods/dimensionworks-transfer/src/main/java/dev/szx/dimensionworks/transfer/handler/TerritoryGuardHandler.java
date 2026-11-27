package dev.szx.dimensionworks.transfer.handler;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritoryBuilder;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import dev.szx.dimensionworks.transfer.territory.PlotChunkLoading;
import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Protects transfer plots against hostile mobs, forbidden blocks, and out-of-bounds edits. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class TerritoryGuardHandler {
    private TerritoryGuardHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.getLevel().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        Territory territory = TerritoryManager.getTerritory(player);
        if (territory == null) {
            return;
        }
        double x = Mth.clamp(player.getX(), territory.interiorMinX() + 0.3D, territory.interiorMaxX() + 0.7D);
        double z = Mth.clamp(player.getZ(), territory.interiorMinZ() + 0.3D, territory.interiorMaxZ() + 0.7D);
        if (x != player.getX() || z != player.getZ()) {
            player.setPos(x, player.getY(), z);
            player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TerritoryBuilder.tick();
        PlotChunkLoading.sendReadyChunks();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TerritoryManager.clear();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        if (event.getState().is(Blocks.BARRIER) || event.getState().is(DispatchBlocks.TERMINAL.get())) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Territory territory = TerritoryManager.getTerritory(player);
        if (territory != null && !territory.contains(event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        boolean forbidden = TransferBlockPolicy.forbidden(level.getBlockState(event.getPos()));
        if (event.getEntity() instanceof Player) {
            forbidden |= TransferBlockPolicy.forbidden(event.getPlacedBlock());
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            forbidden |= multi.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> TransferBlockPolicy.forbidden(snapshot.getCurrentState()));
        }
        if (forbidden) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                TransferBlockPolicy.notifyDenied(player);
            }
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Territory territory = TerritoryManager.getTerritory(player);
        if (territory != null && !territory.contains(event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

}
