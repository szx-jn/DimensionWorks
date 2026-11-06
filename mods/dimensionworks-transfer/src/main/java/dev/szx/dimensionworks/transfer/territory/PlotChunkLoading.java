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
    private record ActivePlot(ServerLevel level, Territory territory) {}
    private PlotChunkLoading() {}

    public static void activate(ServerPlayer player, ServerLevel level, Territory territory) {
        ActivePlot previous = ACTIVE.get(player.getUUID());
        if (previous != null && previous.level() == level && previous.territory().equals(territory)) return;
        deactivate(player.getUUID());
        level.getChunkSource().setViewDistance(2);
        ACTIVE.put(player.getUUID(), new ActivePlot(level, territory));
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
        return territory != null && territory.containsChunk(chunk.x, chunk.z);
    }

    public static void clear() {
        for (UUID player : java.util.List.copyOf(ACTIVE.keySet())) deactivate(player);
    }
}
