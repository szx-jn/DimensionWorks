package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.inventories.InternalInventory;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.core.MemoryDrivePaging;
import dev.szx.dimensionworks.mekstress.core.MemoryDriveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MemoryDriveMenu extends AbstractContainerMenu {
    public static final int SLOTS_PER_PAGE = MemoryDrivePaging.SLOTS_PER_PAGE;

    private final MemoryDriveBlockEntity drive;
    private final Player player;
    private final ContainerData syncData;
    // Slot packets describe the 20 visible slots, not the drive's absolute backing indices.
    private final Container clientSlots = new SimpleContainer(SLOTS_PER_PAGE);
    public int page;

    public MemoryDriveMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, (MemoryDriveBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MemoryDriveMenu(int containerId, Inventory inventory, MemoryDriveBlockEntity drive) {
        super(DimensionWorksMekStress.MEMORY_DRIVE_MENU.get(), containerId);
        this.drive = drive;
        this.player = inventory.player;

        for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
            addSlot(new PagedDriveSlot(this, slot, 8 + MemoryDrivePaging.slotX(slot),
                26 + MemoryDrivePaging.slotY(slot)));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 133 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 193));
        }
        boolean clientSide = inventory.player.level().isClientSide;
        int[] received = new int[MemoryDriveData.SIZE];
        syncData = new ContainerData() {
            @Override
            public int get(int index) {
                if (clientSide) return received[index];
                if (index >= MemoryDriveData.CAPACITY && index < MemoryDriveData.EFFECTIVE)
                    return MemoryDriveData.word(drive.networkCapacitySu(), index - MemoryDriveData.CAPACITY);
                if (index >= MemoryDriveData.EFFECTIVE && index < MemoryDriveData.ALLOCATED)
                    return MemoryDriveData.word(drive.networkEffectiveCapacitySu(), index - MemoryDriveData.EFFECTIVE);
                if (index >= MemoryDriveData.ALLOCATED && index < MemoryDriveData.BANDWIDTH)
                    return MemoryDriveData.word(drive.networkAllocatedSu(), index - MemoryDriveData.ALLOCATED);
                if (index >= MemoryDriveData.BANDWIDTH && index < MemoryDriveData.STATUS)
                    return MemoryDriveData.word(drive.networkBandwidthRpm(), index - MemoryDriveData.BANDWIDTH);
                return switch (index) {
                    case 0 -> drive.networkDriveCount();
                    case MemoryDriveData.STATUS -> drive.networkStatus().ordinal();
                    case MemoryDriveData.CARDS -> drive.cardCount();
                    case MemoryDriveData.PAGE -> page;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (clientSide) {
                    received[index] = value & 0xffff;
                    if (index == MemoryDriveData.PAGE) page = Math.min(received[index], pageCount() - 1);
                }
            }

            @Override
            public int getCount() {
                return MemoryDriveData.SIZE;
            }
        };
        addDataSlots(syncData);
    }

    public MemoryDriveBlockEntity drive() {
        return drive;
    }

    public int driveCount() {
        return syncData.get(0);
    }

    public long networkCapacitySu() {
        return MemoryDriveData.read(syncData::get, MemoryDriveData.CAPACITY);
    }

    public long networkEffectiveCapacitySu() { return MemoryDriveData.read(syncData::get, MemoryDriveData.EFFECTIVE); }
    public long networkAllocatedSu() { return MemoryDriveData.read(syncData::get, MemoryDriveData.ALLOCATED); }
    public long networkAvailableSu() { return Math.max(0, networkEffectiveCapacitySu() - networkAllocatedSu()); }

    public long networkBandwidthRpm() {
        return MemoryDriveData.read(syncData::get, MemoryDriveData.BANDWIDTH);
    }

    public MemoryNetworkStatus status() {
        int ordinal = Math.max(0, Math.min(syncData.get(MemoryDriveData.STATUS), MemoryNetworkStatus.values().length - 1));
        return MemoryNetworkStatus.values()[ordinal];
    }

    public int cardCount() {
        return syncData.get(MemoryDriveData.CARDS);
    }

    public int pageOffset() {
        return MemoryDrivePaging.pageOffset(page);
    }

    public int pageCount() {
        return MemoryDrivePaging.pageCount(drive.tier().slotsPerDrive());
    }

    public boolean isPageSlotActive(int pageSlot) {
        return pageOffset() + pageSlot < drive.tier().slotsPerDrive();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player != this.player || id < 0 || id >= pageCount()) {
            return false;
        }
        page = id;
        // Refresh every visible slot, including empty slots, when changing the backing page.
        broadcastFullState();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS_PER_PAGE) {
            if (!moveItemStackTo(stack, SLOTS_PER_PAGE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int pageIndex = 0; pageIndex < SLOTS_PER_PAGE && !stack.isEmpty(); pageIndex++) {
                Slot target = slots.get(pageIndex);
                if (target.mayPlace(stack)) {
                    ItemStack remainder = target.safeInsert(stack);
                    if (remainder.getCount() != stack.getCount()) {
                        moved = true;
                        stack = remainder;
                    }
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return drive != null
            && !drive.isRemoved()
            && player.distanceToSqr(drive.getBlockPos().getX() + 0.5D, drive.getBlockPos().getY() + 0.5D,
                drive.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    private static final class PagedDriveSlot extends Slot {
        private final MemoryDriveMenu menu;
        private final int pageSlot;

        private PagedDriveSlot(MemoryDriveMenu menu, int pageSlot, int x, int y) {
            super(menu.clientSlots, pageSlot, x, y);
            this.menu = menu;
            this.pageSlot = pageSlot;
        }

        private int inventorySlot() {
            return menu.pageOffset() + pageSlot;
        }

        private InternalInventory inventory() {
            return menu.drive.getInternalInventory();
        }

        @Override
        public ItemStack getItem() {
            if (!isActive()) {
                return ItemStack.EMPTY;
            }
            return menu.player.level().isClientSide
                ? menu.clientSlots.getItem(pageSlot) : inventory().getStackInSlot(inventorySlot());
        }

        @Override
        public void set(ItemStack stack) {
            if (menu.player.level().isClientSide) {
                // Accept authoritative slot content even if the page data packet follows it.
                menu.clientSlots.setItem(pageSlot, stack);
                return;
            }
            if (!isActive()) {
                return;
            }
            if (!stack.isEmpty() && !mayPlace(stack)) {
                return;
            }
            inventory().setItemDirect(inventorySlot(), stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            if (!menu.player.level().isClientSide) menu.drive.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = getItem();
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = current.split(amount);
            set(current);
            return removed;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && menu.drive.isMatchingCard(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return inventorySlot() < menu.drive.tier().slotsPerDrive();
        }
    }
}
