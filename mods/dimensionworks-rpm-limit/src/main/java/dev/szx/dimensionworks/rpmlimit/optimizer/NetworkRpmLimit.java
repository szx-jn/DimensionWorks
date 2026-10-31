package dev.szx.dimensionworks.rpmlimit.optimizer;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember;
import java.util.Map;
import java.util.WeakHashMap;

/** Cache only topology/owner limits, never the current speed or overload flag. */
public final class NetworkRpmLimit {
    private static final Map<KineticNetwork, Integer> LIMITS = new WeakHashMap<>();
    private NetworkRpmLimit() {}
    public static int limit(KineticBlockEntity member) {
        int own = RpmLimitManager.individualLimit(member);
        if (member.getLevel() == null || !member.hasNetwork()) return own;
        if (member.getLevel().isClientSide) {
            int synced = member.getPersistentData().getInt("DWNetworkRpmLimit");
            return synced > 0 ? Math.min(own, synced) : own;
        }
        KineticNetwork network = member.getOrCreateNetwork();
        Integer cached = LIMITS.get(network);
        if (cached == null) {
            int minimum = RpmLimitConfig.MAX_RPM.get();
            for (KineticBlockEntity participant : network.members.keySet()) {
                // Runtime adapters inherit the network's limit; their synthetic BE has no owner.
                if (!(participant instanceof IRuntimeKineticMember))
                    minimum = Math.min(minimum, RpmLimitManager.individualLimit(participant));
            }
            cached = minimum;
            LIMITS.put(network, cached);
        }
        return Math.min(own, cached);
    }
    public static void invalidate(KineticNetwork network) { LIMITS.remove(network); }
}
