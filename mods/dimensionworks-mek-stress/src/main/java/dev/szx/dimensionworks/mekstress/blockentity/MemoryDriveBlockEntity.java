package dev.szx.dimensionworks.mekstress.blockentity;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.block.MemoryDriveBlock;
import dev.szx.dimensionworks.mekstress.core.MemoryDriveStore;
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
    private static final String STORED_SU_KEY = "StoredSu";

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, TOTAL_SLOTS, 1);
    private MemoryTier tier = MemoryTier.DDR1;
    private final MemoryDriveStore store;

    private int networkDriveCount;
    private long networkCapacitySu;
    private long networkStoredSu;
    private long networkBandwidthRpm;
    private MemoryNetworkStatus networkStatus = MemoryNetworkStatus.NO_DRIVE;

    public MemoryDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof MemoryDriveBlock driveBlock) {
            tier = driveBlock.tier();
        }
        store = new MemoryDriveStore(tier, 0);
        store.setCardCapacitySu(MekStressConfig.cardCapacitySu(tier));
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
        store.setCardCapacitySu(MekStressConfig.cardCapacitySu(tier));
        refreshCards();
        store.restore(tag.getLong(STORED_SU_KEY));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(STORED_SU_KEY, store.storedSu());
    }

    public MemoryTier tier() {
        return tier;
    }

    public MemoryDriveStore store() {
        return store;
    }

    public int cardCount() {
        return store.cardCount();
    }

    public void refreshCards() {
        int previousCards = store.cardCount();
        int cards = 0;
        for (int slot = 0; slot < tier.slotsPerDrive(); slot++) {
            if (isMatchingCard(inventory.getStackInSlot(slot))) {
                cards++;
            }
        }
        store.setCardCapacitySu(MekStressConfig.cardCapacitySu(tier));
        store.setCardCount(cards);
        if (cards != previousCards) {
            invalidateNetworkSnapshot();
        }
        setChanged();
    }

    public boolean isMatchingCard(ItemStack stack) {
        return stack.getItem() == DimensionWorksMekStress.memoryCard(tier).get();
    }

    public long capacitySu() {
        store.setCardCapacitySu(MekStressConfig.cardCapacitySu(tier));
        return store.capacitySu();
    }

    public long bandwidthRpm() {
        return MekStressConfig.driveBandwidthRpm(tier);
    }

    public void applyNetworkSnapshot(MemoryNetworkSnapshot snapshot) {
        if (networkDriveCount == snapshot.driveCount()
            && networkCapacitySu == snapshot.capacitySu()
            && networkStoredSu == snapshot.storedSu()
            && networkBandwidthRpm == snapshot.bandwidthRpm()
            && networkStatus == snapshot.status()) {
            return;
        }
        networkDriveCount = snapshot.driveCount();
        networkCapacitySu = snapshot.capacitySu();
        networkStoredSu = snapshot.storedSu();
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
}
