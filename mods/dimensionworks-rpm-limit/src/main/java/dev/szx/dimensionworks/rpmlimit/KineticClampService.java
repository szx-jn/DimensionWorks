package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.api.IPreclampedKineticSpeed;
import dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.optimizer.NetworkRpmLimit;
import dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.UUID;

/** Handles kinetic machine ownership, speed clamping, NBT sync, and optimizer invalidation. */
final class KineticClampService {
    static final String OWNER_KEY = "dimensionworks_owner";
    static final String MACHINE_LIMIT_KEY = "dimensionworks_machine_rpm_limit";
    private static final String SYNC_OWNER_KEY = "DimensionWorksOwner";
    private static final String SYNC_MACHINE_LIMIT_KEY = "DimensionWorksMachineRpmLimit";

    private KineticClampService() {
    }

    static float clamp(KineticBlockEntity k, float requested) {
        if (!Float.isFinite(requested) || requested == 0) {
            return 0;
        }
        if (k instanceof IPreclampedKineticSpeed) {
            return requested;
        }
        return RpmLimitMath.cap(requested, NetworkRpmLimit.limit(k));
    }

    static float clampIndividual(KineticBlockEntity k, float requested, java.util.function.ToIntFunction<UUID> ownerLimitLookup) {
        if (k instanceof IPreclampedKineticSpeed) {
            return Float.isFinite(requested) ? requested : 0;
        }
        return RpmLimitMath.cap(requested, directOwner(k) == null
            ? RpmLimitConfig.MAX_RPM.get()
            : individualLimit(k, ownerLimitLookup));
    }

    static float effectiveScale(KineticBlockEntity k, float raw) {
        if (!Float.isFinite(raw)) {
            return 0;
        }
        if (raw == 0) {
            return 1;
        }
        float effective = clamp(k, raw);
        return Math.min(1, Math.abs(effective / raw));
    }

    static UUID owner(KineticBlockEntity k) {
        KineticBlockEntity cur = k;
        for (int i = 0; i < 32 && cur != null; i++) {
            UUID direct = directOwner(cur);
            if (direct != null) {
                return direct;
            }
            if (!cur.hasSource() || cur.getLevel() == null || !cur.getLevel().hasChunkAt(cur.source)) {
                break;
            }
            BlockEntity blockEntity = cur.getLevel().getBlockEntity(cur.source);
            cur = blockEntity instanceof KineticBlockEntity source ? source : null;
        }
        return null;
    }

    static UUID directOwner(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    static void setOwner(KineticBlockEntity k, UUID u, int effectiveLimit) {
        UUID previousOwner = directOwner(k);
        k.getPersistentData().putUUID(OWNER_KEY, u);
        setMachineLimit(k, effectiveLimit);
        k.setChanged();
        k.sendData();
        if (!Objects.equals(previousOwner, u) && k.getLevel() != null
            && !k.getLevel().isClientSide && k.hasNetwork()) {
            var network = k.getOrCreateNetwork();
            CreateCapacityRegistry.invalidate(network);
            network.updateCapacity();
        }
    }

    static int individualLimit(KineticBlockEntity k, java.util.function.ToIntFunction<UUID> ownerLimitLookup) {
        if (k instanceof IRuntimeKineticMember) {
            return RpmLimitConfig.MAX_RPM.get();
        }
        if (directOwner(k) == null && k.getLevel() != null && k.hasNetwork()) {
            return NetworkRpmLimit.limit(k);
        }
        return limitFor(k, ownerLimitLookup);
    }

    static int limitFor(KineticBlockEntity k, java.util.function.ToIntFunction<UUID> ownerLimitLookup) {
        UUID owner = owner(k);
        if (owner != null && k.getLevel() != null && !k.getLevel().isClientSide) {
            int ownerLimit = ownerLimitLookup.applyAsInt(owner);
            if (ownerLimit > 0) {
                return ownerLimit;
            }
        }

        int stored = storedMachineLimit(k);
        if (stored > 0) {
            return stored;
        }

        KineticBlockEntity current = k;
        for (int i = 0; i < 32 && current != null; i++) {
            int inherited = storedMachineLimit(current);
            if (inherited > 0) {
                return inherited;
            }
            if (!current.hasSource() || current.getLevel() == null || !current.getLevel().hasChunkAt(current.source)) {
                break;
            }
            BlockEntity blockEntity = current.getLevel().getBlockEntity(current.source);
            current = blockEntity instanceof KineticBlockEntity source ? source : null;
        }

        return owner == null ? RpmLimitConfig.DEFAULT_RPM.get() : ownerLimitLookup.applyAsInt(owner);
    }

    static int storedMachineLimit(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.contains(MACHINE_LIMIT_KEY) ? PlayerRpmStore.sanitize(data.getInt(MACHINE_LIMIT_KEY)) : 0;
    }

    static boolean setMachineLimit(KineticBlockEntity k, int limit) {
        int value = PlayerRpmStore.sanitize(limit);
        CompoundTag data = k.getPersistentData();
        int previousLimit = data.getInt(MACHINE_LIMIT_KEY);
        boolean changed = previousLimit != value;
        if (changed) {
            data.putInt(MACHINE_LIMIT_KEY, value);
            k.setChanged();
            KineticNetworkOptimizer.invalidateAndRefreshFor(k);
            if (value > previousLimit) {
                k.updateSpeed = true;
                if (k instanceof GeneratingKineticBlockEntity generator) {
                    generator.reActivateSource = true;
                }
            }
        }

        float currentSpeed = k.getTheoreticalSpeed();
        float cappedSpeed = RpmLimitMath.cap(currentSpeed, value);
        if (currentSpeed != cappedSpeed) {
            k.setSpeed(cappedSpeed);
            k.setChanged();
            k.sendData();
            changed = true;
        }
        return changed;
    }

    static void writeSyncData(KineticBlockEntity k, CompoundTag compound, int machineLimit) {
        UUID direct = directOwner(k);
        if (direct != null) {
            compound.putUUID(SYNC_OWNER_KEY, direct);
        }
        if (machineLimit > 0) {
            compound.putInt(SYNC_MACHINE_LIMIT_KEY, machineLimit);
        }
    }

    static void readSyncData(KineticBlockEntity k, CompoundTag compound) {
        CompoundTag data = k.getPersistentData();
        if (compound.hasUUID(SYNC_OWNER_KEY)) {
            data.putUUID(OWNER_KEY, compound.getUUID(SYNC_OWNER_KEY));
        }
        if (compound.contains(SYNC_MACHINE_LIMIT_KEY)) {
            data.putInt(MACHINE_LIMIT_KEY, PlayerRpmStore.sanitize(compound.getInt(SYNC_MACHINE_LIMIT_KEY)));
        }
    }
}
