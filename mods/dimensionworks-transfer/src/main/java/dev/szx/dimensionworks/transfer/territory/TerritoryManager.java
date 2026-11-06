package dev.szx.dimensionworks.transfer.territory;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Entry point for claiming, building and entering a player's plot. */
public final class TerritoryManager {
    private static final Set<UUID> BUILDING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ENTERING = ConcurrentHashMap.newKeySet();

    private TerritoryManager() {
    }

    public static Territory getTerritory(ServerPlayer player) {
        return TerritorySavedData.get(player.server.overworld()).get(player.getUUID());
    }

    /**
     * Used by the {@code /dwtransfer} command. The plot is claimed and floored before the
     * teleport happens, so the player never lands in unclaimed void.
     */
    public static boolean sendToTransfer(ServerPlayer player) {
        ServerLevel transfer = player.server.getLevel(WDKeys.TRANSFER_LEVEL);
        if (transfer == null) {
            player.displayClientMessage(
                    Component.literal("The dw:transfer dimension is not loaded on this server."), false);
            return false;
        }

        Territory territory = prepare(player, transfer);
        DispatchController.ensure(transfer, territory);
        var landing = SafeLanding.inTerritory(transfer, territory);
        if (landing.isEmpty()) {
            if (player.serverLevel() != transfer) PlotChunkLoading.deactivate(player.getUUID());
            player.displayClientMessage(Component.translatable("message.dimensionworks_transfer.no_safe_landing"), false);
            return false;
        }
        ENTERING.add(player.getUUID());
        try {
            teleportInto(player, transfer, landing.get());
            boolean arrived = player.serverLevel() == transfer;
            if (!arrived) PlotChunkLoading.deactivate(player.getUUID());
            return arrived;
        } finally {
            ENTERING.remove(player.getUUID());
        }
    }

    /** Called whenever a player arrives in the transfer dimension by any other route. */
    public static void enter(ServerPlayer player) {
        if (ENTERING.contains(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        Territory territory = prepare(player, level);
        DispatchController.ensure(level, territory);
        SafeLanding.inTerritory(level, territory).ifPresentOrElse(
            landing -> teleportInto(player, level, landing),
            () -> player.displayClientMessage(Component.translatable("message.dimensionworks_transfer.no_safe_landing"), false));
    }

    /** Claims the plot if needed, loads its chunks and queues the build. Idempotent. */
    private static Territory prepare(ServerPlayer player, ServerLevel level) {
        TerritorySavedData data = TerritorySavedData.get(player.server.overworld());
        Territory territory = data.getOrAssign(player.getUUID());

        PlotChunkLoading.activate(player, level, territory);
        loadPlot(level, territory);

        if (!data.isComplete(player.getUUID()) && BUILDING.add(player.getUUID())) {
            TerritoryBuilder.placeFloor(level, territory);
            TerritoryBuilder.enqueue(level, territory, player.getUUID());
        }
        return territory;
    }

    public static void onBuildFinished(ServerLevel level, UUID playerId) {
        BUILDING.remove(playerId);
        TerritorySavedData.get(level.getServer().overworld()).markComplete(playerId);
    }

    private static void loadPlot(ServerLevel level, Territory territory) {
        for (int dx = 0; dx < Territory.TERRITORY_CHUNKS; dx++) {
            for (int dz = 0; dz < Territory.TERRITORY_CHUNKS; dz++) {
                level.getChunk(territory.chunkX() + dx, territory.chunkZ() + dz);
            }
        }
    }

    private static void teleportInto(ServerPlayer player, ServerLevel level, net.minecraft.core.BlockPos landing) {
        player.teleportTo(level, landing.getX() + .5, landing.getY(), landing.getZ() + .5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setOnGround(true);
    }

    public static void clear() {
        PlotChunkLoading.clear();
        TerritoryBuilder.clear();
        BUILDING.clear();
        ENTERING.clear();
    }
}
