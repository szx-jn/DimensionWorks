package dev.szx.dimensionworks.mekstress.memory;

import com.simibubi.create.content.kinetics.KineticNetwork;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.core.MachinePowerMath;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/** Maintains one virtual Create kinetic member for each powered Mekanism machine. */
public final class MachineKineticRegistry {
    private static final Map<TileEntityMekanism, MachineKineticMember> MEMBERS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<KineticNetwork> DIRTY_NETWORKS =
        Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private MachineKineticRegistry() {
    }

    public static void sync(TileEntityMekanism machine, KineticNetwork network, float memberRpm) {
        sync(machine, network, memberRpm, machine.getBlockPos());
    }

    public static void sync(TileEntityMekanism machine, KineticNetwork network, float memberRpm, net.minecraft.core.BlockPos attachment) {
        if (network == null) {
            detach(machine);
            return;
        }

        float rpm = Float.isFinite(memberRpm) ? Math.max(0.0F, memberRpm) : 0.0F;
        synchronized (MEMBERS) {
            MachineKineticMember member = MEMBERS.computeIfAbsent(machine, MachineKineticMember::new);
            KineticNetwork previous = member.dimensionworks$boundNetwork();
            float previousRpm = member.getTheoreticalSpeed();
            boolean wasAttached = network.members.containsKey(member);
            boolean topologyChanged = previous != network || !wasAttached;
            boolean speedChanged = Float.compare(previousRpm, rpm) != 0;
            member.setSpeed(rpm);
            member.source = attachment.immutable();

            if (previous != network) {
                if (previous != null) {
                    KineticNetworkOptimizer.invalidate(previous);
                    previous.members.remove(member);
                }
                member.dimensionworks$bindNetwork(network);
            }
            if (topologyChanged) {
                KineticNetworkOptimizer.invalidate(network);
                network.members.put(member, MachinePowerMath.stressPerRpm(1.0F));
            } else if (speedChanged) {
                // Virtual members do not tick in the world, so their own KineticBlockEntity
                // dirty flag cannot drive the refresh. Update the aggregate directly when cached.
                KineticNetworkOptimizer.updateStressContribution(network, member);
            }

            if (previous != null && previous != network) {
                KineticNetworkOptimizer.invalidate(previous);
                schedule(previous);
            }
            if (topologyChanged || speedChanged) {
                if (topologyChanged)
                    KineticNetworkOptimizer.invalidate(network);
                schedule(network);
            }
        }
    }

    public static void detach(TileEntityMekanism machine) {
        synchronized (MEMBERS) {
            MachineKineticMember member = MEMBERS.remove(machine);
            if (member == null) {
                return;
            }
            KineticNetwork network = member.dimensionworks$boundNetwork();
            member.dimensionworks$unbindNetwork();
            if (network != null) {
                KineticNetworkOptimizer.invalidate(network);
                network.members.remove(member);
                schedule(network);
            }
        }
    }

    static void flushDirtyNetworks() {
        KineticNetworkOptimizer.abortBatch();
        ArrayList<KineticNetwork> pending;
        synchronized (DIRTY_NETWORKS) {
            pending = new ArrayList<>(DIRTY_NETWORKS);
            DIRTY_NETWORKS.clear();
        }
        for (KineticNetwork network : pending) {
            try {
                network.updateNetwork();
            } catch (RuntimeException exception) {
                DimensionWorksMekStress.LOGGER.warn(
                    "Failed to refresh a Mekanism-backed kinetic network.",
                    exception);
            }
        }
    }

    private static void schedule(KineticNetwork network) {
        if (network != null) {
            DIRTY_NETWORKS.add(network);
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            flushDirtyNetworks();
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        DIRTY_NETWORKS.clear();
    }
}
