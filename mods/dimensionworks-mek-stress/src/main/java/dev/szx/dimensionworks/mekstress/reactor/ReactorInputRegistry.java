package dev.szx.dimensionworks.mekstress.reactor;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mirrors the Mekanism virtual-machine member, without fabricating FE.
 * Create reserves two SU per supplied FE/t against the injector's actual network.
 */
final class ReactorInputRegistry {
    private static final Map<TileReactorCore, ReactorInputMember> MEMBERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private ReactorInputRegistry() {
    }

    static void sync(TileReactorCore core, TileReactorInjector injector,
                     KineticBlockEntity shaft, KineticNetwork network, long fePerTick) {
        float rpm = Math.abs(dev.szx.dimensionworks.rpmlimit.RpmLimitManager.clamp(shaft, shaft.getTheoreticalSpeed()));
        synchronized (MEMBERS) {
            ReactorInputMember member = MEMBERS.get(core);
            if (member != null && (!member.dimensionworks$isValid()
                || !member.getBlockPos().equals(injector.getBlockPos()))) {
                removeMember(member);
                member = null;
            }
            if (member == null) {
                member = new ReactorInputMember(core, injector);
                MEMBERS.put(core, member);
            }

            KineticNetwork previous = member.boundNetwork();
            boolean topologyChanged = previous != network || !network.members.containsKey(member);
            boolean speedChanged = Float.compare(member.getTheoreticalSpeed(), rpm) != 0;
            float coefficient = dev.szx.dimensionworks.mekstress.core.EnergyPowerMath.demandCoefficient(fePerTick * 2.0D, rpm);
            boolean loadChanged = Float.compare(member.calculateStressApplied(), coefficient) != 0;
            member.setCoefficient(coefficient);

            if (previous != null && previous != network) {
                member.unbind();
                previous.members.remove(member);
                KineticNetworkOptimizer.invalidate(previous);
                previous.updateNetwork();
            }

            member.setSpeed(rpm);
            member.source = shaft.getBlockPos().immutable();
            member.bind(network);
            if (topologyChanged) {
                network.members.put(member, coefficient);
                KineticNetworkOptimizer.invalidate(network);
                network.updateNetwork();
            } else if (speedChanged || loadChanged) {
                network.members.put(member, coefficient);
                KineticNetworkOptimizer.updateStressContribution(network, member);
                network.updateNetwork();
            }
        }
    }

    static double availableSu(TileReactorCore core, KineticNetwork network) {
        ReactorInputMember member = MEMBERS.get(core);
        double own = member != null && member.boundNetwork() == network && network.members.containsKey(member)
            ? member.calculateStressApplied() * (double) Math.abs(member.getTheoreticalSpeed()) : 0;
        double free = network.calculateCapacity() - (double) network.calculateStress() + own;
        return Double.isFinite(free) && free > 0 ? Math.max(0, Math.nextDown((float) free)) : 0;
    }

    static void detach(TileReactorCore core) {
        synchronized (MEMBERS) {
            ReactorInputMember member = MEMBERS.remove(core);
            if (member != null) {
                removeMember(member);
            }
        }
    }

    private static void removeMember(ReactorInputMember member) {
        KineticNetwork network = member.boundNetwork();
        member.unbind();
        if (network == null) {
            return;
        }
        network.members.remove(member);
        KineticNetworkOptimizer.invalidate(network);
        network.updateNetwork();
    }
}
