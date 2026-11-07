package dev.szx.dimensionworks.transfer.territory;

import dev.szx.dimensionworks.transfer.WDKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/** Nine live plot tickets replace the dimension's ordinary player view-distance tickets. */
public final class PlotChunkLoading {
    private static final TicketType<UUID> PLOT = TicketType.create("dw_transfer_plot", UUID::compareTo);
    private static final Map<UUID, ActivePlot> ACTIVE = new HashMap<>();
    private record ActivePlot(ServerLevel level, Territory territory, java.util.Set<Long> sent) {}
    private PlotChunkLoading() {}

    public static void activate(ServerPlayer player, ServerLevel level, Territory territory) {
        ActivePlot previous = ACTIVE.get(player.getUUID());
        if (previous != null && previous.level() == level && previous.territory().equals(territory)) return;
        deactivate(player.getUUID());
        level.getChunkSource().setViewDistance(2);
        ACTIVE.put(player.getUUID(), new ActivePlot(level, territory, new java.util.HashSet<>()));
        for (int x = 0; x < Territory.TERRITORY_CHUNKS; x++) {
            for (int z = 0; z < Territory.TERRITORY_CHUNKS; z++) {
                level.getChunkSource().addRegionTicket(PLOT,
                    new ChunkPos(territory.chunkX() + x, territory.chunkZ() + z), 2, player.getUUID(), true);
            }
        }
    }

    public static void deactivate(UUID player) {
        ActivePlot active = ACTIVE.remove(player);
        if (active == null) return;
        Territory territory = active.territory();
        for (int x = 0; x < Territory.TERRITORY_CHUNKS; x++) {
            for (int z = 0; z < Territory.TERRITORY_CHUNKS; z++) {
                active.level().getChunkSource().removeRegionTicket(PLOT,
                    new ChunkPos(territory.chunkX() + x, territory.chunkZ() + z), 2, player, true);
            }
        }
    }

    public static boolean maySend(ServerPlayer player, ChunkPos chunk) {
        if (!player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) return true;
        Territory territory = TerritoryManager.getTerritory(player);
        return territory != null && territory.containsRenderChunk(chunk.x, chunk.z);
    }

    /** Fixed plot coverage, independent of the player standing near a plot edge. */
    public static void sendReadyChunks() {
        for (var entry : ACTIVE.entrySet()) {
            ActivePlot active = entry.getValue();
            ServerPlayer player = active.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel() != active.level()) continue;
            if (active.sent().size() == 25) continue;
            Territory territory = active.territory();
            for (int x = territory.chunkX() - 1; x <= territory.chunkX() + 3; x++) {
                for (int z = territory.chunkZ() - 1; z <= territory.chunkZ() + 3; z++) {
                    long key = ChunkPos.asLong(x, z);
                    if (active.sent().contains(key)) continue;
                    var chunk = active.level().getChunkSource().getChunkNow(x, z);
                    if (chunk == null || !chunk.isLightCorrect()) continue;
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                        chunk, active.level().getChunkSource().getLightEngine(), null, null));
                    active.sent().add(key);
                }
            }
        }
    }

    public static void clear() {
        for (UUID player : java.util.List.copyOf(ACTIVE.keySet())) deactivate(player);
    }
}
