package dev.szx.dimensionworks.mekstress.blockentity;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.block.MemoryDriveBlock;
import dev.szx.dimensionworks.mekstress.card.MemoryCardItem;
import dev.szx.dimensionworks.mekstress.core.MemoryDriveMath;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class MemoryDriveBlockEntity extends AENetworkInvBlockEntity {
    public static final int TOTAL_SLOTS = 64;

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, TOTAL_SLOTS, 1);
    private MemoryTier tier = MemoryTier.DDR1;
    private int cardCount;
    private long bandwidthRpm;
    private long inputReductionBasisPoints;

    private int networkDriveCount;
    private long networkCapacitySu;
    private long networkStoredSu;
    private long networkEffectiveCapacitySu;
    private long networkAllocatedSu;
    private long networkBandwidthRpm;
    private MemoryNetworkStatus networkStatus = MemoryNetworkStatus.NO_DRIVE;

    public MemoryDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof MemoryDriveBlock driveBlock) {
            tier = driveBlock.tier();
        }
        bandwidthRpm = MekStressConfig.driveBandwidthRpm(tier);
        inventory.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
                return slot >= 0 && slot < tier.slotsPerDrive() && isMatchingCard(stack);
            }
        });
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return super.createMainNode()
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setIdlePowerUsage(1.0D);
    }

    @Override
    public Set<Direction> getGridConnectableSides(appeng.api.orientation.BlockOrientation orientation) {
        return Set.of(Direction.values());
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inventory;
    }

    @Override
    public void onChangeInventory(InternalInventory inventory, int slot) {
        refreshCards();
        invalidateNetworkSnapshot();
        markForUpdate();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State state) {
        super.onMainNodeStateChanged(state);
        invalidateNetworkSnapshot();
    }

    @Override
    public void setRemoved() {
        invalidateNetworkSnapshot();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        invalidateNetworkSnapshot();
        super.onChunkUnloaded();
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        refreshCards();
        // Old StoredSu represented energy. Live mechanical capacity is never restored from NBT.
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.remove("StoredSu");
    }

    public MemoryTier tier() {
        return tier;
    }

    public int cardCount() {
        return cardCount;
    }

    public void refreshCards() {
        int previousCards = cardCount;
        long previousBandwidthRpm = bandwidthRpm;
        long previousInputReductionBasisPoints = inputReductionBasisPoints;
        int cards = 0;
        long bandwidthDeltaRpm = 0L;
        long totalInputReductionBasisPoints = 0L;
        long baseCardBandwidthRpm = MekStressConfig.cardBandwidthRpm(tier);
        long baseDriveBandwidthRpm = MekStressConfig.driveBandwidthRpm(tier);

        for (int slot = 0; slot < tier.slotsPerDrive(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof MemoryCardItem card) || card.tier() != tier) {
                continue;
            }
            cards++;
            bandwidthDeltaRpm = saturatedAdd(bandwidthDeltaRpm, MemoryDriveMath.scaleByPowerOfTwo(
                baseCardBandwidthRpm, MekStressConfig.speedExponent(card.type())));
            totalInputReductionBasisPoints = saturatedAdd(
                totalInputReductionBasisPoints, card.type().inputReductionBasisPoints());
        }
        bandwidthRpm = saturatedAdd(baseDriveBandwidthRpm, bandwidthDeltaRpm);
        cardCount = cards;
        inputReductionBasisPoints = totalInputReductionBasisPoints;
        if (cards != previousCards
            || bandwidthRpm != previousBandwidthRpm
            || inputReductionBasisPoints != previousInputReductionBasisPoints) {
            invalidateNetworkSnapshot();
        }
        setChanged();
    }

    public boolean isMatchingCard(ItemStack stack) {
        return stack.getItem() instanceof MemoryCardItem card && card.tier() == tier;
    }

    public long bandwidthRpm() {
        return bandwidthRpm;
    }

    public long inputReductionBasisPoints() {
        return inputReductionBasisPoints;
    }

    public void applyNetworkSnapshot(MemoryNetworkSnapshot snapshot) {
        if (networkDriveCount == snapshot.driveCount()
            && networkCapacitySu == snapshot.capacitySu()
            && networkStoredSu == snapshot.availableSu()
            && networkEffectiveCapacitySu == snapshot.effectiveCapacitySu()
            && networkAllocatedSu == snapshot.allocatedSu()
            && networkBandwidthRpm == snapshot.bandwidthRpm()
            && networkStatus == snapshot.status()) {
            return;
        }
        networkDriveCount = snapshot.driveCount();
        networkCapacitySu = snapshot.capacitySu();
        networkStoredSu = snapshot.availableSu();
        networkEffectiveCapacitySu = snapshot.effectiveCapacitySu();
        networkAllocatedSu = snapshot.allocatedSu();
        networkBandwidthRpm = snapshot.bandwidthRpm();
        networkStatus = snapshot.status();
        markForUpdate();
    }

    public int networkDriveCount() {
        return networkDriveCount;
    }

    public long networkCapacitySu() {
        return networkCapacitySu;
    }

    public long networkStoredSu() {
        return networkStoredSu;
    }

    public long networkEffectiveCapacitySu() { return networkEffectiveCapacitySu; }
    public long networkAllocatedSu() { return networkAllocatedSu; }

    public long networkBandwidthRpm() {
        return networkBandwidthRpm;
    }

    public MemoryNetworkStatus networkStatus() {
        return networkStatus;
    }

    private void invalidateNetworkSnapshot() {
        if (level == null || level.isClientSide || getMainNode().getGrid() == null) {
            return;
        }
        IMemoryGridService service = getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service != null) {
            service.invalidateSnapshot();
        }
    }

    private static long saturatedAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) {
            return Long.MAX_VALUE;
        }
        if (b < 0L && a < Long.MIN_VALUE - b) {
            return Long.MIN_VALUE;
        }
        return a + b;
    }
}
