package dev.szx.dimensionworks.mekstress.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/** Allocates already-paid live SU, without connecting AE grids or loading chunks. */
public final class CrossDimensionStressAllocator {
    public enum Priority { HIGH, NORMAL, LOW }
    public enum Status { OK, NO_INPUT, RPM_MISMATCH, INSUFFICIENT, NUMERIC_LIMIT }

    public record Input(String dimension, long capacity, float rpm) {
        public Input {
            Objects.requireNonNull(dimension);
            if (dimension.isEmpty() || capacity < 0 || !Float.isFinite(rpm) || rpm < 0) throw new IllegalArgumentException();
        }
    }

    public record Output(String id, String dimension, long demand, Priority priority) {
        public Output {
            Objects.requireNonNull(id);
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(priority);
            if (id.isEmpty() || dimension.isEmpty() || demand < 0) throw new IllegalArgumentException();
        }
    }

    public record Result(Status status, float rpm, Set<String> running) {
        public Result { running = Set.copyOf(running); }
    }

    private CrossDimensionStressAllocator() {}

    /** Each priority is atomic: a failing group and all lower priorities stop. */
    public static Result allocate(List<Input> inputs, List<Output> outputs) {
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(outputs);
        Set<String> ids = new LinkedHashSet<>();
        for (Output output : outputs) {
            if (!ids.add(output.id())) throw new IllegalArgumentException("Duplicate output ID");
        }
        Map<String, Long> available = new LinkedHashMap<>();
        float rpm = 0;
        for (Input input : inputs) {
            if (input.capacity() == 0 || input.rpm() == 0) continue;
            if (rpm != 0 && rpm != input.rpm()) return new Result(Status.RPM_MISMATCH, 0, Set.of());
            rpm = input.rpm();
            // Fail closed instead of saturating: saturation could hide an overcommit.
            try {
                available.merge(input.dimension(), input.capacity(), Math::addExact);
            } catch (ArithmeticException overflow) {
                return new Result(Status.NUMERIC_LIMIT, 0, Set.of());
            }
        }
        if (rpm == 0) return new Result(Status.NO_INPUT, 0, Set.of());
        Set<String> running = new LinkedHashSet<>();
        List<Output> accepted = new ArrayList<>();
        for (Priority priority : Priority.values()) {
            List<Output> group = outputs.stream()
                .filter(o -> o.priority() == priority && o.demand() > 0).toList();
            if (group.isEmpty()) continue;
            List<Output> proposed = new ArrayList<>(accepted);
            proposed.addAll(group);
            // Recompute earlier allocations so their arbitrary matching does not starve lower priorities.
            if (allocateGroup(available, proposed) == null) return new Result(Status.INSUFFICIENT, rpm, running);
            accepted = proposed;
            group.forEach(output -> running.add(output.id()));
        }
        return new Result(Status.OK, rpm, running);
    }

    /** Maximum flow avoids a greedy allocation starving an otherwise feasible output. */
    private static Map<String, Long> allocateGroup(Map<String, Long> available, List<Output> group) {
        List<String> dimensions = new ArrayList<>(available.keySet());
        int source = 0, firstInput = 1, firstOutput = firstInput + dimensions.size();
        int sink = firstOutput + group.size();
        Flow flow = new Flow(sink + 1);
        List<Edge> inputEdges = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            String dimension = dimensions.get(i);
            long capacity = available.get(dimension);
            inputEdges.add(flow.add(source, firstInput + i, capacity));
            for (int j = 0; j < group.size(); j++) {
                Output output = group.get(j);
                if (!dimension.equals(output.dimension())) {
                    flow.add(firstInput + i, firstOutput + j, Math.min(capacity, output.demand()));
                }
            }
        }
        List<Edge> outputEdges = new ArrayList<>();
        for (int j = 0; j < group.size(); j++) {
            outputEdges.add(flow.add(firstOutput + j, sink, group.get(j).demand()));
        }
        flow.run(source, sink);
        if (outputEdges.stream().anyMatch(edge -> edge.remaining != 0)) return null;
        Map<String, Long> used = new LinkedHashMap<>();
        for (int i = 0; i < dimensions.size(); i++) {
            Edge edge = inputEdges.get(i);
            used.put(dimensions.get(i), available.get(dimensions.get(i)) - edge.remaining);
        }
        return used;
    }

    private static final class Edge {
        final int to;
        final int reverse;
        long remaining;
        Edge(int to, int reverse, long remaining) { this.to = to; this.reverse = reverse; this.remaining = remaining; }
    }

    private static final class Flow {
        final List<List<Edge>> graph;
        Flow(int size) {
            graph = new ArrayList<>(size);
            for (int i = 0; i < size; i++) graph.add(new ArrayList<>());
        }
        Edge add(int from, int to, long capacity) {
            Edge forward = new Edge(to, graph.get(to).size(), capacity);
            Edge reverse = new Edge(from, graph.get(from).size(), 0);
            graph.get(from).add(forward);
            graph.get(to).add(reverse);
            return forward;
        }
        void run(int source, int sink) {
            // Edmonds-Karp gives deterministic polynomial behaviour for the small endpoint graph.
            while (true) {
                int[] previous = new int[graph.size()];
                int[] edgeIndex = new int[graph.size()];
                Arrays.fill(previous, -1);
                previous[source] = source;
                ArrayDeque<Integer> queue = new ArrayDeque<>();
                queue.add(source);
                while (!queue.isEmpty() && previous[sink] < 0) {
                    int at = queue.remove();
                    for (int i = 0; i < graph.get(at).size(); i++) {
                        Edge edge = graph.get(at).get(i);
                        if (edge.remaining == 0 || previous[edge.to] >= 0) continue;
                        previous[edge.to] = at;
                        edgeIndex[edge.to] = i;
                        queue.add(edge.to);
                    }
                }
                if (previous[sink] < 0) return;
                long amount = Long.MAX_VALUE;
                for (int at = sink; at != source; at = previous[at]) {
                    amount = Math.min(amount, graph.get(previous[at]).get(edgeIndex[at]).remaining);
                }
                for (int at = sink; at != source; at = previous[at]) {
                    Edge edge = graph.get(previous[at]).get(edgeIndex[at]);
                    edge.remaining -= amount;
                    graph.get(at).get(edge.reverse).remaining += amount;
                }
            }
        }
    }
}
