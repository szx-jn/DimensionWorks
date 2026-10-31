package dev.szx.dimensionworks.rpmlimit.diagnostics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

/** Counts source-propagation edges without inventing adjacency for unloaded or foreign nodes. */
public final class TopologyEdges {
    public record Summary(int sourceEdges, int resolvableEdges, int unresolvedParents) {}

    private TopologyEdges() {}

    public static Map<BlockPos, Integer> indices(List<NetworkSnapshot.Node> nodes) {
        Map<BlockPos, Integer> indices = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) indices.putIfAbsent(nodes.get(i).pos(), i);
        return indices;
    }

    public static Summary summarize(List<NetworkSnapshot.Node> nodes) {
        Set<BlockPos> positions = indices(nodes).keySet();

        int sourceEdges = 0;
        int resolvableEdges = 0;
        int unresolvedParents = 0;
        for (NetworkSnapshot.Node node : nodes) {
            if (node.parent() == null) continue;
            sourceEdges++;
            if (positions.contains(node.parent())) resolvableEdges++;
            else unresolvedParents++;
        }
        return new Summary(sourceEdges, resolvableEdges, unresolvedParents);
    }
}
