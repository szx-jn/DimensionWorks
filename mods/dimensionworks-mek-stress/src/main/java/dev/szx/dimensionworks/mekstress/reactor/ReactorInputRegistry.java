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
 * Create charges 256 SU per RPM against the injector's actual input network.
 */
final class ReactorInputRegistry {
    private static final Map<TileReactorCore, ReactorInputMember> MEMBERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private ReactorInputRegistry() {
    }

    static void sync(TileReactorCore core, TileReactorInjector injector,
                     KineticBlockEntity shaft, KineticNetwork network) {
        float rpm = Math.abs(shaft.getTheoreticalSpeed());
        synchronized (MEMBERS) {
            ReactorInputMember member = MEMBERS.get(core);
            if (member != null && (!member.dimensionworks$isValid()
                || !member.getBlockPos().equals(injector.getBlockPos()))) {
                removeMember(member);
                member = null;
            }
            if (member == null) {
                member = new ReactorInputMember(injector);
                MEMBERS.put(core, member);
            }

            KineticNetwork previous = member.boundNetwork();
            boolean topologyChanged = previous != network || !network.members.containsKey(member);
            boolean speedChanged = Float.compare(member.getTheoreticalSpeed(), rpm) != 0;

            if (previous != null && previous != network) {
                member.unbind();
                previous.members.remove(member);
                KineticNetworkOptimizer.invalidate(previous);
                previous.updateNetwork();
            }

            member.setSpeed(rpm);
            member.bind(network);
            if (topologyChanged) {
                network.members.put(member, ReactorPowerBridge.INPUT_STRESS_PER_RPM);
                KineticNetworkOptimizer.invalidate(network);
                network.updateNetwork();
            } else if (speedChanged) {
                KineticNetworkOptimizer.updateStressContribution(network, member);
                network.updateNetwork();
            }
        }
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
