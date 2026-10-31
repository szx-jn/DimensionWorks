package dev.szx.dimensionworks.rpmlimit.optimizer;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember;
import dev.szx.dimensionworks.rpmlimit.mixin.KineticNetworkAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;

/** Versioned physical-member accounting, independent of runtime quotas and virtual consumers. */
public final class KineticPersistence {
    private static final String KEY = "DWKineticAccounting";
    private static final Map<KineticBlockEntity, Saved> SAVED = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<KineticNetwork, Totals> SNAPSHOTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private record Saved(float capacity, float stress, int count, float ownCapacity, float ownStress) {}
    private record Totals(long tick, float capacity, float stress, int count) {}
    private KineticPersistence() {}
    public static void invalidate(KineticNetwork network) { SNAPSHOTS.remove(network); }
    public static void read(KineticBlockEntity member, CompoundTag nbt, boolean clientPacket) {
        if (clientPacket) return;
        SAVED.remove(member);
        CompoundTag tag = nbt.getCompound(KEY);
        if (tag.getInt("Version") != 1) return;
        float c = tag.getFloat("Capacity"), s = tag.getFloat("Stress");
        float oc = tag.getFloat("OwnCapacity"), os = tag.getFloat("OwnStress");
        int count = tag.getInt("Members");
        if (valid(c) && valid(s) && valid(oc) && valid(os) && count >= 0)
            SAVED.put(member, new Saved(c, s, count, oc, os));
    }
    private static boolean valid(float value) { return Float.isFinite(value) && value >= 0; }
    public static void initialize(KineticBlockEntity member, KineticNetwork network) {
        Saved saved = SAVED.get(member);
        // Legacy aggregates cannot distinguish phantom/virtual/physical unloaded load. Rebuild
        // from members as their chunks load, preserving the existing rotation/source graph.
        network.initFromTE(saved == null ? 0 : saved.capacity, saved == null ? 0 : saved.stress, saved == null ? 0 : saved.count);
    }
    public static void restore(KineticNetwork network, KineticBlockEntity member) {
        if (network.members.containsKey(member)) return;
        KineticNetworkAccessor state = (KineticNetworkAccessor) network;
        Saved saved = SAVED.remove(member);
        if (saved != null) {
            state.dimensionworks$setUnloadedCapacity(Math.max(0, state.dimensionworks$unloadedCapacity() - saved.ownCapacity));
            state.dimensionworks$setUnloadedStress(Math.max(0, state.dimensionworks$unloadedStress() - saved.ownStress));
            state.dimensionworks$setUnloadedMembers(Math.max(0, state.dimensionworks$unloadedMembers() - 1));
        }
        if (state.dimensionworks$unloadedMembers() == 0) {
            state.dimensionworks$setUnloadedCapacity(0);
            state.dimensionworks$setUnloadedStress(0);
        }
        if (member.isSource()) network.sources.put(member, member.calculateAddedStressCapacity());
        network.members.put(member, member.calculateStressApplied());
        KineticNetworkOptimizer.onMemberLoadedSilently(network, member);
    }
    public static void write(KineticBlockEntity member, CompoundTag nbt, boolean clientPacket) {
        if (clientPacket || member.getLevel() == null || member.getLevel().isClientSide || !member.hasNetwork()) return;
        KineticNetwork network = member.getOrCreateNetwork();
        long tick = member.getLevel().getGameTime();
        Totals totals = SNAPSHOTS.get(network);
        if (totals == null || totals.tick != tick) {
            KineticNetworkAccessor state = (KineticNetworkAccessor) network;
            float capacity = state.dimensionworks$unloadedCapacity(), stress = state.dimensionworks$unloadedStress();
            int count = state.dimensionworks$unloadedMembers();
            for (KineticBlockEntity physical : network.members.keySet()) {
                if (physical instanceof IRuntimeKineticMember) continue;
                stress += network.getActualStressOf(physical);
                if (network.sources.containsKey(physical)) capacity += network.getActualCapacityOf(physical);
                count++;
            }
            totals = new Totals(tick, capacity, stress, count);
            SNAPSHOTS.put(network, totals);
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1); tag.putFloat("Capacity", totals.capacity); tag.putFloat("Stress", totals.stress);
        tag.putInt("Members", totals.count);
        tag.putFloat("OwnCapacity", network.sources.containsKey(member) ? network.getActualCapacityOf(member) : 0);
        tag.putFloat("OwnStress", network.members.containsKey(member) ? network.getActualStressOf(member) : 0);
        nbt.put(KEY, tag);
    }
}
