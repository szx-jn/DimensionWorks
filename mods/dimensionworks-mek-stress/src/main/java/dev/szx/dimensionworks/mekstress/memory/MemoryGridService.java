package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.config.Actionable;
import appeng.api.networking.GridServices;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import com.loliball.appliedcreate.storage.StressKey;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.mekstress.core.NetworkMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/** One ME Memory service per logical AE2 grid. */
public final class MemoryGridService implements IMemoryGridService, IStorageProvider {
    private static final long CONSUMER_TIMEOUT_TICKS = 3L;

    private final IGrid grid;
    private final Map<GlobalPos, GearboxDemand> gearboxDemands = new HashMap<>();
    private boolean mounted;
    private long lastComputedTick = Long.MIN_VALUE;
    private MemoryNetworkSnapshot lastSnapshot = MemoryNetworkSnapshot.empty(MemoryNetworkStatus.NO_DRIVE);

    private final MEStorage storage = new MEStorage() {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, appeng.api.networking.security.IActionSource source) {
            if (what != StressKey.Companion.getINSTANCE()) {
                return 0L;
            }
            long accepted = insertSu(amount, mode, currentTick());
            if (mode == Actionable.MODULATE && accepted > 0L) {
                invalidateStorage();
            }
            return accepted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, appeng.api.networking.security.IActionSource source) {
            if (what != StressKey.Companion.getINSTANCE()) {
                return 0L;
            }
            long extracted = extractSu(amount, mode, currentTick());
            if (mode == Actionable.MODULATE && extracted > 0L) {
                invalidateStorage();
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            long stored = snapshot(currentTick()).storedSu();
            if (stored > 0L) {
                out.add(StressKey.Companion.getINSTANCE(), stored);
            }
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gui.dimensionworks_mek_stress.memory_storage");
        }
    };

    public MemoryGridService(IGrid grid) {
        this.grid = grid;
    }

    public static void register() {
        GridServices.register(IMemoryGridService.class, MemoryGridService.class);
    }

    @Override
    public void onServerStartTick() {
        ensureMounted();
        snapshot(currentTick());
    }

    @Override
    public void onJoin(appeng.api.networking.IGridStorage storage) {
        ensureMounted();
        invalidateSnapshot();
    }

    @Override
    public void onSplit(appeng.api.networking.IGridStorage storage) {
        ensureMounted();
        invalidateSnapshot();
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        mounts.mount(storage);
    }

    @Override
    public MemoryNetworkSnapshot snapshot(long gameTick) {
        long tick = normalizedTick(gameTick);
        if (lastComputedTick == tick) {
            return lastSnapshot;
        }
        expire(tick);
        List<MemoryDriveBlockEntity> drives = findDrives();
        boolean tooMany = drives.size() > MemoryTier.MAX_DRIVES_PER_NETWORK;
        MemoryNetworkStatus status = drives.isEmpty()
            ? MemoryNetworkStatus.NO_DRIVE
            : tooMany ? MemoryNetworkStatus.TOO_MANY_DRIVES : MemoryNetworkStatus.OK;

        long capacity = 0L;
        long stored = 0L;
        long bandwidth = 0L;
        if (!tooMany) {
            for (MemoryDriveBlockEntity drive : drives) {
                long driveCapacity = drive.capacitySu();
                long driveStored = Math.min(drive.store().storedSu(), driveCapacity);
                if (drive.store().storedSu() != driveStored) {
                    drive.store().restore(driveStored);
                    drive.setChanged();
                }
                capacity = saturatedAdd(capacity, driveCapacity);
                stored = saturatedAdd(stored, driveStored);
                bandwidth = saturatedAdd(bandwidth, drive.bandwidthRpm());
            }
        }

        long demandRpm = 0L;
        long demandSu = 0L;
        for (GearboxDemand demand : gearboxDemands.values()) {
            demandRpm = saturatedAdd(demandRpm, demand.requestedRpm);
            demandSu = saturatedAdd(demandSu, demand.requestedSuPerTick);
        }

        double stockQ = status == MemoryNetworkStatus.TOO_MANY_DRIVES
            ? 0.0D
            : NetworkMath.stockRatio(stored, demandSu);
        double bandwidthQ = status == MemoryNetworkStatus.TOO_MANY_DRIVES
            ? 0.0D
            : NetworkMath.bandwidthQ(bandwidth, demandRpm);
        if (status == MemoryNetworkStatus.NO_DRIVE && (demandRpm > 0L || demandSu > 0L)) {
            stockQ = 0.0D;
            bandwidthQ = 0.0D;
        }
        double q = NetworkMath.finalQ(stockQ, bandwidthQ);

        MemoryNetworkSnapshot snapshot = new MemoryNetworkSnapshot(
            drives.size(), capacity, stored, bandwidth, demandRpm, demandSu, stockQ, bandwidthQ, q, status);
        lastSnapshot = snapshot;
        lastComputedTick = tick;
        for (MemoryDriveBlockEntity drive : drives) {
            drive.applyNetworkSnapshot(snapshot);
        }
        return snapshot;
    }

    @Override
    public long insertSu(long amount, Actionable mode, long gameTick) {
        if (amount <= 0L) {
            return 0L;
        }
        MemoryNetworkSnapshot snapshot = snapshot(gameTick);
        if (snapshot.status() != MemoryNetworkStatus.OK) {
            return 0L;
        }
        long remaining = amount;
        long accepted = 0L;
        for (MemoryDriveBlockEntity drive : findDrives()) {
            long capacity = drive.capacitySu();
            long room = Math.max(0L, capacity - drive.store().storedSu());
            long amountForDrive = Math.min(room, remaining);
            if (amountForDrive <= 0L) {
                continue;
            }
            if (mode == Actionable.MODULATE) {
                long inserted = drive.store().insert(amountForDrive);
                if (inserted > 0L) {
                    drive.setChanged();
                }
                accepted = saturatedAdd(accepted, inserted);
                remaining -= inserted;
            } else {
                accepted = saturatedAdd(accepted, amountForDrive);
                remaining -= amountForDrive;
            }
            if (remaining <= 0L) {
                break;
            }
        }
        if (mode == Actionable.MODULATE && accepted > 0L) {
            updateSnapshotStorage(accepted);
        }
        return accepted;
    }

    @Override
    public long extractSu(long amount, Actionable mode, long gameTick) {
        if (amount <= 0L) {
            return 0L;
        }
        MemoryNetworkSnapshot snapshot = snapshot(gameTick);
        if (snapshot.status() != MemoryNetworkStatus.OK) {
            return 0L;
        }
        long remaining = amount;
        long extracted = 0L;
        for (MemoryDriveBlockEntity drive : findDrives()) {
            long available = drive.store().storedSu();
            long amountForDrive = Math.min(available, remaining);
            if (amountForDrive <= 0L) {
                continue;
            }
            if (mode == Actionable.MODULATE) {
                long taken = drive.store().extract(amountForDrive);
                if (taken > 0L) {
                    drive.setChanged();
                }
                extracted = saturatedAdd(extracted, taken);
                remaining -= taken;
            } else {
                extracted = saturatedAdd(extracted, amountForDrive);
                remaining -= amountForDrive;
            }
            if (remaining <= 0L) {
                break;
            }
        }
        if (mode == Actionable.MODULATE && extracted > 0L) {
            updateSnapshotStorage(-extracted);
        }
        return extracted;
    }

    @Override
    public void reportGearboxExport(GlobalPos gearboxPos, int requestedRpm, long requestedSuPerTick, long gameTick) {
        long tick = normalizedTick(gameTick);
        int rpm = Math.max(0, requestedRpm);
        long suPerTick = Math.max(0L, requestedSuPerTick);
        GearboxDemand previous = gearboxDemands.get(gearboxPos);
        boolean changed = previous == null || previous.lastSeenTick < tick - CONSUMER_TIMEOUT_TICKS
            || previous.requestedRpm != rpm || previous.requestedSuPerTick != suPerTick;
        gearboxDemands.put(gearboxPos, new GearboxDemand(rpm, suPerTick, tick));
        if (changed) {
            lastComputedTick = Long.MIN_VALUE;
        }
    }

    @Override
    public double finalQ(long gameTick) {
        return snapshot(gameTick).finalQ();
    }

    @Override
    public void invalidateSnapshot() {
        lastComputedTick = Long.MIN_VALUE;
    }

    private void updateSnapshotStorage(long delta) {
        if (lastComputedTick == Long.MIN_VALUE || lastSnapshot.status() != MemoryNetworkStatus.OK) {
            return;
        }
        long stored = lastSnapshot.storedSu();
        if (delta >= 0L) {
            stored = saturatedAdd(stored, delta);
        } else {
            stored = Math.max(0L, stored + delta);
        }
        stored = Math.min(stored, lastSnapshot.capacitySu());
        double stockQ = NetworkMath.stockRatio(stored, lastSnapshot.demandSuPerTick());
        double bandwidthQ = NetworkMath.bandwidthQ(lastSnapshot.bandwidthRpm(), lastSnapshot.demandRpm());
        double q = NetworkMath.finalQ(stockQ, bandwidthQ);
        lastSnapshot = new MemoryNetworkSnapshot(
            lastSnapshot.driveCount(), lastSnapshot.capacitySu(), stored, lastSnapshot.bandwidthRpm(),
            lastSnapshot.demandRpm(), lastSnapshot.demandSuPerTick(), stockQ, bandwidthQ, q, lastSnapshot.status());
    }

    private void ensureMounted() {
        if (mounted) {
            return;
        }
        IStorageService storageService = grid.getStorageService();
        if (storageService != null) {
            storageService.addGlobalStorageProvider(this);
            mounted = true;
        }
    }

    private void invalidateStorage() {
        IStorageService storageService = grid.getStorageService();
        if (storageService != null) {
            storageService.invalidateCache();
        }
    }

    private long currentTick() {
        IGridNode pivot = grid.getPivot();
        if (pivot != null && pivot.getLevel() != null) {
            return pivot.getLevel().getGameTime();
        }
        return lastComputedTick == Long.MIN_VALUE ? 0L : lastComputedTick;
    }

    private long normalizedTick(long gameTick) {
        return gameTick == Long.MIN_VALUE ? currentTick() : gameTick;
    }

    private void expire(long gameTick) {
        gearboxDemands.values().removeIf(demand -> gameTick - demand.lastSeenTick > CONSUMER_TIMEOUT_TICKS);
    }

    private List<MemoryDriveBlockEntity> findDrives() {
        Map<GlobalPos, MemoryDriveBlockEntity> drives = new LinkedHashMap<>();
        for (IGridNode node : grid.getNodes()) {
            Object owner = node.getOwner();
            if (!(owner instanceof MemoryDriveBlockEntity drive)) {
                continue;
            }
            GlobalPos globalPos = GlobalPos.of(drive.getLevel().dimension(), drive.getBlockPos());
            drives.put(globalPos, drive);
        }
        List<MemoryDriveBlockEntity> result = new ArrayList<>(drives.values());
        result.sort(Comparator
            .comparing((MemoryDriveBlockEntity drive) -> drive.getLevel().dimension().location().toString())
            .thenComparingInt(drive -> drive.getBlockPos().getX())
            .thenComparingInt(drive -> drive.getBlockPos().getY())
            .thenComparingInt(drive -> drive.getBlockPos().getZ()));
        return result;
    }

    private static long saturatedAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private record GearboxDemand(int requestedRpm, long requestedSuPerTick, long lastSeenTick) {
    }
}
