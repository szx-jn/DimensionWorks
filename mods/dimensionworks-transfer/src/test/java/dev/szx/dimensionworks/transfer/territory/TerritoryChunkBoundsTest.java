package dev.szx.dimensionworks.transfer.territory;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerritoryChunkBoundsTest {
    @Test void excludesGapsAndAdjacentPlotsAtNegativeCoordinates() {
        for (int gridX = -3; gridX <= 3; gridX++) {
            Territory territory = new Territory(0, gridX, -gridX);
            int accepted = 0;
            for (int x = territory.chunkX() - 2; x <= territory.chunkX() + 5; x++)
                for (int z = territory.chunkZ() - 2; z <= territory.chunkZ() + 5; z++)
                    if (territory.containsChunk(x, z)) {
                        accepted++;
                        assertTrue(territory.contains(x * 16, z * 16));
                        assertTrue(territory.contains(x * 16 + 15, z * 16 + 15));
                    }
            assertEquals(9, accepted);
            assertFalse(territory.containsChunk(territory.chunkX() + Territory.STRIDE_CHUNKS, territory.chunkZ()));
        }
    }
}
