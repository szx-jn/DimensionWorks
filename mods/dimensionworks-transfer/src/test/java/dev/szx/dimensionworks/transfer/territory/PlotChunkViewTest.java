package dev.szx.dimensionworks.transfer.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class PlotChunkViewTest {
    @Test void coversExactlyThePlotAndRenderBorderAtNegativeCoordinates() {
        var view = new PlotChunkView(-11, 23);
        var chunks = new HashSet<ChunkPos>(); view.forEach(chunks::add);
        assertEquals(25, chunks.size());
        for (var chunk : chunks) assertTrue(view.contains(chunk));
        int ticking = 0;
        for (int x = -13; x < -6; x++) for (int z = 21; z < 28; z++) {
            assertEquals(chunks.contains(new ChunkPos(x, z)), view.contains(x, z));
            if (view.isInViewDistance(x, z)) ticking++;
        }
        assertEquals(9, ticking);
    }
    @Test void nativeViewDifferenceOnlyRemovesThePreviousPlotAndAddsTheNewPlot() {
        var oldView = new PlotChunkView(0, 0); var newView = new PlotChunkView(12, -12);
        var added = new HashSet<ChunkPos>(); var removed = new HashSet<ChunkPos>();
        net.minecraft.server.level.ChunkTrackingView.difference(oldView, newView, added::add, removed::add);
        assertEquals(25, added.size()); assertEquals(25, removed.size());
        for (var chunk : added) assertTrue(newView.contains(chunk) && !oldView.contains(chunk));
        for (var chunk : removed) assertTrue(oldView.contains(chunk) && !newView.contains(chunk));
    }
}
