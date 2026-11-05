package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.core.CrossDimensionStressAllocator;
import dev.szx.dimensionworks.mekstress.core.CrossDimensionStressAllocator.Input;
import dev.szx.dimensionworks.mekstress.core.CrossDimensionStressAllocator.Output;
import dev.szx.dimensionworks.mekstress.core.CrossDimensionStressAllocator.Priority;
import dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/** Updates only loaded endpoint block entities registered by their own load/tick lifecycle. */
public final class CrossDimensionTransportManager {
    private static final Set<CrossDimensionTransportBlockEntity> ENDPOINTS =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<CrossDimensionTransportBlockEntity, UUID> IDS = new IdentityHashMap<>();
    private CrossDimensionTransportManager() {}

    public static synchronized void register(CrossDimensionTransportBlockEntity endpoint) {
        ENDPOINTS.add(endpoint);
        IDS.computeIfAbsent(endpoint, ignored -> UUID.randomUUID());
    }

    public static synchronized void unregister(CrossDimensionTransportBlockEntity endpoint) {
        ENDPOINTS.remove(endpoint);
        IDS.remove(endpoint);
        endpoint.applyPowerOutput(0, 0);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        List<CrossDimensionTransportBlockEntity> snapshot;
        synchronized (CrossDimensionTransportManager.class) { snapshot = List.copyOf(ENDPOINTS); }
        Map<Integer, List<CrossDimensionTransportBlockEntity>> byChannel = new java.util.HashMap<>();
        for (CrossDimensionTransportBlockEntity endpoint : snapshot) {
            if (!endpoint.isLive()) continue;
            if (endpoint.channel() > 0) byChannel.computeIfAbsent(endpoint.channel(), ignored -> new ArrayList<>()).add(endpoint);
            else endpoint.applyPowerOutput(0, 0);
        }

        for (List<CrossDimensionTransportBlockEntity> group : byChannel.values()) {
            CrossDimensionTransportBlockEntity firstEndpoint = group.get(0);
            if (firstEndpoint.getLevel() instanceof ServerLevel serverLevel) {
                int tier = ChannelDirectorySavedData.get(serverLevel).tierForChannel(firstEndpoint.channel());
                ChannelStockSavedData.get(serverLevel).tickChannel(firstEndpoint.channel(),
                    serverLevel.getServer().getTickCount(), tier);
            }
            List<Input> inputs = new ArrayList<>();
            List<CrossDimensionTransportBlockEntity> outputEndpoints = new ArrayList<>();
            float inputRpm = 0;
            for (CrossDimensionTransportBlockEntity endpoint : group) {
                if (endpoint.isPowerInput()) {
                    var network = endpoint.powerNetwork();
                    float rpm = endpoint.liveRpm();
                    if (network != null && rpm > 0 && !endpoint.isOverStressed()
                        && !network.sources.keySet().stream().anyMatch(source ->
                            source instanceof CrossDimensionTransportBlockEntity other && other.isPowerOutput())) {
                        long su = endpoint.inputSu(rpm);
                        inputs.add(new Input(endpoint.dimensionId(), su, rpm));
                        if (inputRpm == 0) inputRpm = rpm;
                    }
                } else if (endpoint.isPowerOutput()) {
                    outputEndpoints.add(endpoint);
                }
            }

            List<Output> outputs = new ArrayList<>();
            if (inputRpm > 0) {
                for (CrossDimensionTransportBlockEntity endpoint : outputEndpoints) {
                    outputs.add(new Output(idOf(endpoint), endpoint.dimensionId(), endpoint.outputSu(inputRpm),
                        Priority.values()[endpoint.priority()]));
                }
            }
            var allocation = CrossDimensionStressAllocator.allocate(inputs, outputs);
            for (CrossDimensionTransportBlockEntity endpoint : outputEndpoints) {
                String id = idOf(endpoint);
                boolean running = allocation.running().contains(id);
                float rpm = running ? allocation.rpm() : 0;
                endpoint.applyPowerOutput(rpm, running ? endpoint.outputSu(rpm) : 0);
            }
        }
    }

    private static synchronized String idOf(CrossDimensionTransportBlockEntity endpoint) {
        return IDS.computeIfAbsent(endpoint, ignored -> UUID.randomUUID()).toString();
    }

    public static synchronized void onServerStopping(ServerStoppingEvent event) {
        ENDPOINTS.forEach(endpoint -> endpoint.applyPowerOutput(0, 0));
        ENDPOINTS.clear();
        IDS.clear();
    }
}
