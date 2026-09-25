package dev.szx.dimensionworks.transfer.territory;

import dev.szx.dimensionworks.transfer.WDKeys;
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

    private TerritoryManager() {
    }

    public static Territory getTerritory(ServerPlayer player) {
        return TerritorySavedData.get(player.server.overworld()).get(player.getUUID());
    }

    /**
     * Used by the {@code /dwtransfer} command. The plot is claimed and floored before the
     * teleport happens, so the player never lands in unclaimed void.
     */
    public static void sendToTransfer(ServerPlayer player) {
        ServerLevel transfer = player.server.getLevel(WDKeys.TRANSFER_LEVEL);
        if (transfer == null) {
            player.displayClientMessage(
                    Component.literal("The dw:transfer dimension is not loaded on this server."), false);
            return;
        }

        Territory territory = prepare(player, transfer);
        teleportInto(player, transfer, territory);
    }

    /** Called whenever a player arrives in the transfer dimension by any other route. */
    public static void enter(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        Territory territory = prepare(player, level);
        teleportInto(player, level, territory);
    }

    /** Claims the plot if needed, loads its chunks and queues the build. Idempotent. */
    private static Territory prepare(ServerPlayer player, ServerLevel level) {
        TerritorySavedData data = TerritorySavedData.get(player.server.overworld());
        Territory territory = data.getOrAssign(player.getUUID());

        forceLoad(level, territory);

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

    private static void forceLoad(ServerLevel level, Territory territory) {
        for (int dx = 0; dx < Territory.TERRITORY_CHUNKS; dx++) {
            for (int dz = 0; dz < Territory.TERRITORY_CHUNKS; dz++) {
                level.getChunk(territory.chunkX() + dx, territory.chunkZ() + dz);
            }
        }
    }

    private static void teleportInto(ServerPlayer player, ServerLevel level, Territory territory) {
        player.teleportTo(level, territory.centerX(), Territory.FLOOR_Y + 1, territory.centerZ(),
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setOnGround(true);
    }
}
