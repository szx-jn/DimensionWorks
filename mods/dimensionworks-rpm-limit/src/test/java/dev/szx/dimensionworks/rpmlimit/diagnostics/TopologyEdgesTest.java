package dev.szx.dimensionworks.rpmlimit.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TopologyEdgesTest {

    @Test
    void virtualChildrenKeepTheirSourceEdges() {
        BlockPos parent = new BlockPos(1, 2, 3);
        NetworkSnapshot.Node virtualChild = new NetworkSnapshot.Node(
            new BlockPos(2, 2, 3), parent, "create:test", null,
            16.0F, 16.0F, 32, 0.0F, 0.0F, false, true);

        TopologyEdges.Summary summary = TopologyEdges.summarize(List.of(virtualChild));

        assertEquals(1, summary.sourceEdges());
        assertEquals(0, summary.resolvableEdges());
        assertEquals(1, summary.unresolvedParents());
    }

    @Test
    void parentsInsideTheSnapshotAreResolvable() {
        BlockPos parent = new BlockPos(1, 2, 3);
        NetworkSnapshot.Node root = new NetworkSnapshot.Node(
            parent, null, "create:root", null,
            16.0F, 16.0F, 32, 0.0F, 1.0F, false, false);
        NetworkSnapshot.Node child = new NetworkSnapshot.Node(
            new BlockPos(2, 2, 3), parent, "create:child", null,
            16.0F, 16.0F, 32, 0.0F, 0.0F, false, true);

        TopologyEdges.Summary summary = TopologyEdges.summarize(List.of(root, child));

        assertEquals(1, summary.sourceEdges());
        assertEquals(1, summary.resolvableEdges());
        assertEquals(0, summary.unresolvedParents());
    }

    @Test
    void virtualNodesRemainInThePositionIndexUsedByTheGraph() {
        BlockPos childPos = new BlockPos(2, 2, 3);
        NetworkSnapshot.Node virtualChild = new NetworkSnapshot.Node(
            childPos, new BlockPos(1, 2, 3), "create:child", null,
            16.0F, 16.0F, 32, 0.0F, 0.0F, false, true);

        assertNotNull(TopologyEdges.indices(List.of(virtualChild)).get(childPos));
    }
}
