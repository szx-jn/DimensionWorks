package dev.szx.dimensionworks.transfer.territory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RenderHaloTest {
    @Test void allNineChunksHaveEightNeighboursIncludingNegativePlots() {
        for (int grid : new int[]{-2, 0, 2}) {
            Territory t = new Territory(0, grid, grid);
            int count = 0;
            for (int x = t.chunkX()-2; x <= t.chunkX()+4; x++) {
                for (int z = t.chunkZ()-2; z <= t.chunkZ()+4; z++) {
                    if (t.containsRenderChunk(x,z)) count++;
                    if (!t.containsChunk(x,z)) continue;
                    for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++)
                        assertTrue(t.containsRenderChunk(x+dx,z+dz));
                }
            }
            assertEquals(25,count);
            assertFalse(t.containsChunk(t.chunkX()-1,t.chunkZ()));
            assertFalse(t.containsRenderChunk(t.chunkX()+Territory.STRIDE_CHUNKS,t.chunkZ()));
        }
    }
}
