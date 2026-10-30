package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.networking.GridServices;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.IMEGearboxExportState;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.core.InputStressMath;
import dev.szx.dimensionworks.mekstress.core.LiveStressBudget;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;

/** Live allocations for one AE grid. No StressKey inventory and no SU/t debit. */
public final class MemoryGridService implements IMemoryGridService {
    private final IGrid grid;
    private final Set<IGridNode> participants = new LinkedHashSet<>();
    private boolean initialized;
    private boolean refreshing;
    private MemoryNetworkSnapshot lastSnapshot = MemoryNetworkSnapshot.empty(MemoryNetworkStatus.NO_DRIVE);

    public MemoryGridService(IGrid grid) { this.grid = grid; }

    public static void register() { GridServices.register(IMemoryGridService.class, MemoryGridService.class); }

    @Override public void addNode(IGridNode node, CompoundTag savedData) { track(node); }

    @Override public void removeNode(IGridNode node) {
        participants.remove(node);
        boolean previous = refreshing;
        refreshing = true;
        try {
            if (node.getOwner() instanceof IMEGearboxExportState gearbox)
                gearbox.dimensionworks$applyExportState(false, 0, 0, true);
        } finally {
            refreshing = previous;
        }
    }

    @Override public void onServerStartTick() { snapshot(0); }
    @Override public void onServerEndTick() { snapshot(0); }

    private void track(IGridNode node) {
        if (node.getOwner() instanceof MEGearboxBlockEntity || node.getOwner() instanceof MemoryDriveBlockEntity)
            participants.add(node);
    }

    @Override public MemoryNetworkSnapshot snapshot(long gameTick) {
        if (refreshing) return lastSnapshot;
        refreshing = true;
        try {
            if (!initialized) {
                for (IGridNode node : grid.getNodes()) track(node);
                initialized = true;
            }
            var budget = new LiveStressBudget<GlobalPos>();
            List<MemoryDriveBlockEntity> drives = new ArrayList<>();
            List<Export> exports = new ArrayList<>();
            long bandwidth = 0;
            long requestedRpm = 0;
            long reduction = 0;
            for (IGridNode node : List.copyOf(participants)) {
                Object owner = node.getOwner();
                if (owner instanceof MemoryDriveBlockEntity drive) {
                    if (!node.isActive() || drive.isRemoved() || drive.getLevel() == null) continue;
                    drives.add(drive);
                    bandwidth = add(bandwidth, drive.bandwidthRpm());
                    reduction = add(reduction, drive.inputReductionBasisPoints());
                } else if (owner instanceof MEGearboxBlockEntity gearbox) {
                    if (gearbox.getLevel() == null || gearbox.isRemoved()) continue;
                    GlobalPos key = GlobalPos.of(gearbox.getLevel().dimension(), gearbox.getBlockPos());
                    if (gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
                        int rpm = node.isActive() ? MachinePowerManager.effectiveGearboxRpm(gearbox) : 0;
                        long reserved = LiveStressBudget.stress(gearbox.getConfiguredStress(), rpm);
                        budget.output(key, reserved);
                        requestedRpm = add(requestedRpm, reserved > 0 ? rpm : 0);
                        exports.add(new Export(gearbox, rpm, reserved));
                    } else if (node.isActive()) budget.input(key, inputCapacity(gearbox));
                }
            }
            int loss = Math.min(MekStressConfig.maximumInputLossBasisPoints(),
                InputStressMath.cappedReductionBasisPoints(reduction));
            LiveStressBudget.Snapshot allocation = budget.snapshot(loss);
            MemoryNetworkStatus status = drives.isEmpty() ? MemoryNetworkStatus.NO_DRIVE
                : drives.size() > MemoryTier.MAX_DRIVES_PER_NETWORK ? MemoryNetworkStatus.TOO_MANY_DRIVES
                : allocation.overloaded() ? MemoryNetworkStatus.OUTPUT_OVERLOAD
                : requestedRpm > bandwidth ? MemoryNetworkStatus.BANDWIDTH_OVERLOAD : MemoryNetworkStatus.OK;
            lastSnapshot = new MemoryNetworkSnapshot(drives.size(), allocation.rawSu(), allocation.effectiveSu(),
                allocation.reservedSu(), bandwidth, requestedRpm, status);
            for (Export output : exports) {
                boolean running = status == MemoryNetworkStatus.OK && output.rpm > 0 && output.reserved > 0;
                ((IMEGearboxExportState) (Object) output.gearbox).dimensionworks$applyExportState(
                    running, output.rpm, output.reserved, status != MemoryNetworkStatus.OK);
            }
            for (MemoryDriveBlockEntity drive : drives) drive.applyNetworkSnapshot(lastSnapshot);
            return lastSnapshot;
        } finally { refreshing = false; }
    }

    private static long inputCapacity(MEGearboxBlockEntity gearbox) {
        if (!gearbox.hasSource() || gearbox.isOverStressed()) return 0;
        var network = gearbox.getOrCreateNetwork();
        // ME output must not be recycled as a fresh input, even through a second AE grid.
        if (network == null || network.sources.keySet().stream().anyMatch(source ->
            source instanceof MEGearboxBlockEntity me && me.getMode() == MEGearboxBlockEntity.Mode.EXPORT)) return 0;
        float rpm = RpmLimitManager.clamp(gearbox, gearbox.getTheoreticalSpeed());
        rpm = Math.min(Math.abs(rpm), MekStressConfig.directMaxRpm());
        return LiveStressBudget.stress(gearbox.getConfiguredStress(), rpm);
    }

    @Override public double finalQ(long gameTick) { return snapshot(gameTick).finalQ(); }
    @Override public void invalidateSnapshot() { if (!refreshing) snapshot(0); }

    private static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    private record Export(MEGearboxBlockEntity gearbox, int rpm, long reserved) {}
}
