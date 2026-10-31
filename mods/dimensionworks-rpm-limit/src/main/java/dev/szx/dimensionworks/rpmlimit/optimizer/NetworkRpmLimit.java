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
        if (member.getLevel() == null || !member.hasNetwork()) return RpmLimitManager.fallbackIndividualLimit(member);
        if (member.getLevel().isClientSide) {
            int synced = member.getPersistentData().getInt("DWNetworkRpmLimit");
            return synced > 0 ? synced : RpmLimitManager.fallbackIndividualLimit(member);
        }
        KineticNetwork network = member.getOrCreateNetwork();
        Integer cached = LIMITS.get(network);
        if (cached == null) {
            int minimum = Integer.MAX_VALUE;
            for (KineticBlockEntity participant : network.members.keySet()) {
                // Runtime adapters inherit the network's limit; their synthetic BE has no owner.
                if (!(participant instanceof IRuntimeKineticMember) && RpmLimitManager.directOwner(participant) != null
                    && participant.getLevel() != null
                    && !participant.isRemoved() && participant.getLevel().hasChunkAt(participant.getBlockPos())
                    && participant.getLevel().getBlockEntity(participant.getBlockPos()) == participant)
                    minimum = Math.min(minimum, RpmLimitManager.individualLimit(participant));
            }
            cached = minimum == Integer.MAX_VALUE ? RpmLimitConfig.DEFAULT_RPM.get() : minimum;
            LIMITS.put(network, cached);
        }
        return RpmLimitManager.directOwner(member) == null ? cached
            : Math.min(RpmLimitManager.fallbackIndividualLimit(member), cached);
    }
    public static void invalidate(KineticNetwork network) { LIMITS.remove(network); }
}
