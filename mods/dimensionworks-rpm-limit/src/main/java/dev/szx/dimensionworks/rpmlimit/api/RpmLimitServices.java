package dev.szx.dimensionworks.rpmlimit.api;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.OverspeedCurve;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyNetwork;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import dev.szx.dimensionworks.rpmlimit.quota.FixedIntervalTokenBucket;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartOwnerResolver;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartQuotaPolicy;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartQuotaType;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.GearHeartState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Narrow cross-mod service facade for {@code dimensionworks_rpm_limit}.
 * External modules should depend on {@code dev.szx.dimensionworks.rpmlimit.api} rather than
 * importing internal managers, optimizers, or quota registries directly.
 */
public final class RpmLimitServices {
    private static final IRpmLimitService RPM = new DefaultRpmLimitService();
    private static final IKineticOptimizerService OPTIMIZER = new DefaultKineticOptimizerService();
    private static final IOverspeedService OVERSPEED = new DefaultOverspeedService();
    private static final IQuotaGateService QUOTA = new DefaultQuotaGateService();
    private static final ITopologyAdapterRegistry TOPOLOGY = new DefaultTopologyAdapterRegistry();

    private RpmLimitServices() {
    }

    public static IRpmLimitService rpm() {
        return RPM;
    }

    public static IKineticOptimizerService optimizer() {
        return OPTIMIZER;
    }

    public static IOverspeedService overspeed() {
        return OVERSPEED;
    }

    public static IQuotaGateService quota() {
        return QUOTA;
    }

    public static ITopologyAdapterRegistry topology() {
        return TOPOLOGY;
    }

    private static final class DefaultRpmLimitService implements IRpmLimitService {
        @Override
        public float clamp(KineticBlockEntity kinetic, float requestedSpeed) {
            return RpmLimitManager.clamp(kinetic, requestedSpeed);
        }

        @Override
        public UUID owner(KineticBlockEntity kinetic) {
            return RpmLimitManager.owner(kinetic);
        }

        @Override
        public int effectiveLimit(UUID owner) {
            return RpmLimitManager.getEffectiveLimit(owner);
        }

        @Override
        public void setLimit(ServerPlayer player, int rpm) {
            RpmLimitManager.setLimit(player, rpm);
        }

        @Override
        public void setOwner(KineticBlockEntity kinetic, UUID owner) {
            RpmLimitManager.setOwner(kinetic, owner);
        }
    }

    private static final class DefaultKineticOptimizerService implements IKineticOptimizerService {
        @Override
        public Float cachedStress(KineticNetwork network) {
            return KineticNetworkOptimizer.cachedStress(network);
        }

        @Override
        public void beginStressRebuild(KineticNetwork network) {
            KineticNetworkOptimizer.beginStressRebuild(network);
        }

        @Override
        public void recordStressContribution(KineticNetwork network, KineticBlockEntity member, float contribution) {
            KineticNetworkOptimizer.recordStressContribution(network, member, contribution);
        }

        @Override
        public void finishStressRebuild(KineticNetwork network, float totalStress) {
            KineticNetworkOptimizer.finishStressRebuild(network, totalStress);
        }

        @Override
        public boolean updateStressContribution(KineticNetwork network, KineticBlockEntity member) {
            return KineticNetworkOptimizer.updateStressContribution(network, member);
        }

        @Override
        public boolean updateCapacityContribution(KineticNetwork network, KineticBlockEntity source) {
            return KineticNetworkOptimizer.updateCapacityContribution(network, source);
        }

        @Override
        public void onMemberPruned(KineticNetwork network, KineticBlockEntity member) {
            KineticNetworkOptimizer.onMemberPruned(network, member);
        }

        @Override
        public void invalidate(KineticNetwork network) {
            KineticNetworkOptimizer.invalidate(network);
        }

        @Override
        public void invalidateAndRefreshFor(KineticBlockEntity member) {
            KineticNetworkOptimizer.invalidateAndRefreshFor(member);
        }

        @Override
        public void beginBatch() {
            KineticNetworkOptimizer.beginBatch();
        }

        @Override
        public void endBatch() {
            KineticNetworkOptimizer.endBatch();
        }

        @Override
        public void abortBatch() {
            KineticNetworkOptimizer.abortBatch();
        }
    }

    private static final class DefaultOverspeedService implements IOverspeedService {
        @Override
        public boolean isEnabled() {
            return RpmLimitConfig.OVERSPEED_ENABLED.get();
        }

        @Override
        public boolean isStressScalingEnabled() {
            return RpmLimitConfig.OVERSPEED_STRESS_SCALING.get();
        }

        @Override
        public boolean isWhitelisted(KineticBlockEntity kinetic) {
            return OverspeedBonus.isWhitelisted(kinetic);
        }

        @Override
        public double multiplier(double rpm, double saturationRpm) {
            return OverspeedCurve.multiplier(rpm, saturationRpm, RpmLimitConfig.OVERSPEED_CAP.get());
        }

        @Override
        public int batches(double multiplier, long tick) {
            return OverspeedCurve.batches(multiplier, tick);
        }
    }

    private static final class DefaultQuotaGateService implements IQuotaGateService {
        @Override
        public UUID sharedOwner() {
            return GearHeartOwnerResolver.SHARED_OWNER;
        }

        @Override
        public UUID normalizeOwner(UUID owner) {
            return GearHeartOwnerResolver.normalize(owner);
        }

        @Override
        public boolean hasQuota(UUID owner) {
            return GearHeartEffects.hasQuota(owner);
        }

        @Override
        public int activeQuotaMask(UUID owner) {
            return GearHeartEffects.activeQuotaMask(owner);
        }

        @Override
        public Set<UUID> unreleasedOwners(QuotaFamily family) {
            return GearHeartEffects.unreleasedOwners(toRepairBit(family));
        }

        @Override
        public boolean isRepairApplied(int repairedMask, QuotaFamily family) {
            return GearHeartState.isRepairApplied(repairedMask, toRepairBit(family));
        }

        @Override
        public double configuredRateLimit(int repairedMask, QuotaFamily family) {
            GearHeartQuotaType type = toQuotaType(family);
            double policy = GearHeartQuotaPolicy.limit(type, repairedMask);
            if (Double.isInfinite(policy)) {
                return policy;
            }
            return switch (family) {
                case MEK_ORE_OPERATIONS -> GearHeartState.isRepairApplied(repairedMask, GearHeartState.MEK_ORE_OPERATIONS)
                    ? Double.POSITIVE_INFINITY
                    : Math.min(RpmLimitConfig.GEAR_HEART_MEK_ORE_OPERATIONS_CURSED_PER_SECOND.get(), policy);
                case CROSS_PROCESS -> Math.min(
                    GearHeartState.isRepairApplied(repairedMask, GearHeartState.CROSS_PROCESS)
                        ? RpmLimitConfig.GEAR_HEART_CROSS_PROCESS_REPAIRED_PER_SECOND.get()
                        : RpmLimitConfig.GEAR_HEART_CROSS_PROCESS_CURSED_PER_SECOND.get(),
                    policy);
                case ACTIVE_MACHINES -> Math.min(
                    GearHeartState.isRepairApplied(repairedMask, GearHeartState.ACTIVE_MACHINES)
                        ? RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_REPAIRED.get()
                        : RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_CURSED.get(),
                    policy);
                case FINAL_PRODUCTION -> Math.min(
                    GearHeartState.isRepairApplied(repairedMask, GearHeartState.FINAL_PRODUCTION)
                        ? RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND.get()
                        : RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND.get(),
                    policy);
                default -> policy;
            };
        }

        @Override
        public boolean tryQuotaPlan(Collection<QuotaRequest> requests) {
            if (requests == null || requests.isEmpty()) {
                return true;
            }
            List<FixedIntervalTokenBucket.QuotaRequest> internal = new ArrayList<>(requests.size());
            for (QuotaRequest req : requests) {
                internal.add(new FixedIntervalTokenBucket.QuotaRequest(
                    req.owner(), toQuotaType(req.family()), req.amount(), req.perSecond(), req.tick()));
            }
            return GearHeartEffects.tryQuotaPlan(internal);
        }

        @Override
        public boolean tryConsumeFinalItem(UUID owner, ItemStack output, long amount, long tick) {
            return FinalProductionGate.tryConsumeItem(owner, output, amount, tick);
        }

        @Override
        public boolean tryConsumeFinalFluid(UUID owner, FluidStack output, long amount, long tick) {
            return FinalProductionGate.tryConsume(owner, output, amount, tick);
        }

        @Override
        public boolean tryConsumeFinalTagged(UUID owner, boolean taggedAsFinalProduction, long amount, long tick) {
            return FinalProductionGate.tryConsumeTagged(owner, taggedAsFinalProduction, amount, tick);
        }

        private static GearHeartQuotaType toQuotaType(QuotaFamily family) {
            return switch (family) {
                case ROTATION_SPEED -> GearHeartQuotaType.ROTATION_SPEED;
                case CREATE_CAPACITY -> GearHeartQuotaType.CREATE_CAPACITY;
                case MEK_ORE_OPERATIONS -> GearHeartQuotaType.MEK_ORE_OPERATIONS;
                case AE_CRAFTING_JOBS -> GearHeartQuotaType.AE_CRAFTING_JOBS;
                case CROSS_PROCESS -> GearHeartQuotaType.CROSS_PROCESS;
                case ACTIVE_MACHINES -> GearHeartQuotaType.ACTIVE_MACHINES;
                case FINAL_PRODUCTION -> GearHeartQuotaType.FINAL_PRODUCTION;
            };
        }

        private static int toRepairBit(QuotaFamily family) {
            return switch (family) {
                case ROTATION_SPEED -> GearHeartState.ROTATION_SPEED;
                case CREATE_CAPACITY -> GearHeartState.CREATE_CAPACITY;
                case MEK_ORE_OPERATIONS -> GearHeartState.MEK_ORE_OPERATIONS;
                case AE_CRAFTING_JOBS -> GearHeartState.AE_CRAFTING_JOBS;
                case CROSS_PROCESS -> GearHeartState.CROSS_PROCESS;
                case ACTIVE_MACHINES -> GearHeartState.ACTIVE_MACHINES;
                case FINAL_PRODUCTION -> GearHeartState.FINAL_PRODUCTION;
            };
        }
    }

    private static final class DefaultTopologyAdapterRegistry implements ITopologyAdapterRegistry {
        @Override
        public void registerAdapter(Predicate<BlockEntity> matches, Function<BlockEntity, KineticBlockEntity> resolve) {
            TopologyNetwork.registerAdapter(matches, resolve);
        }
    }
}
