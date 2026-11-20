package dev.szx.dimensionworks.transfer.territory;

import java.util.function.Consumer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

/** Exactly the three-by-three plot and its one-chunk render border. */
public record PlotChunkView(int chunkX, int chunkZ) implements ChunkTrackingView {
    @Override public boolean contains(int x, int z, boolean includeBorder) {
        int border = includeBorder ? 1 : 0;
        return x >= chunkX - border && x < chunkX + Territory.TERRITORY_CHUNKS + border
            && z >= chunkZ - border && z < chunkZ + Territory.TERRITORY_CHUNKS + border;
    }
    @Override public void forEach(Consumer<ChunkPos> consumer) {
        for (int x = chunkX - 1; x <= chunkX + Territory.TERRITORY_CHUNKS; x++)
            for (int z = chunkZ - 1; z <= chunkZ + Territory.TERRITORY_CHUNKS; z++) consumer.accept(new ChunkPos(x, z));
    }
}
