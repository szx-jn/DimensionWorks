package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.inventories.InternalInventory;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
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
    public static final int SLOTS_PER_PAGE = 10;
    public static final int MAX_PAGE = 6;

    private final MemoryDriveBlockEntity drive;
    private final Player player;
    private final ContainerData syncData;
    public int page;

    public MemoryDriveMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, (MemoryDriveBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MemoryDriveMenu(int containerId, Inventory inventory, MemoryDriveBlockEntity drive) {
        super(DimensionWorksMekStress.MEMORY_DRIVE_MENU.get(), containerId);
        this.drive = drive;
        this.player = inventory.player;

        for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
            addSlot(new PagedDriveSlot(this, slot, 16 + (slot % 5) * 22, 26 + (slot / 5) * 22));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 16 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 16 + col * 18, 142));
        }
        syncData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> drive.networkDriveCount();
                    case 1 -> (int) (drive.networkCapacitySu() >>> 32);
                    case 2 -> (int) drive.networkCapacitySu();
                    case 3 -> (int) (drive.networkStoredSu() >>> 32);
                    case 4 -> (int) drive.networkStoredSu();
                    case 5 -> (int) (drive.networkBandwidthRpm() >>> 32);
                    case 6 -> (int) drive.networkBandwidthRpm();
                    case 7 -> drive.networkStatus().ordinal();
                    case 8 -> drive.cardCount();
                    case 9 -> 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 10;
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
        return ((long) syncData.get(1) << 32) | (syncData.get(2) & 0xffffffffL);
    }

    public long networkStoredSu() {
        return ((long) syncData.get(3) << 32) | (syncData.get(4) & 0xffffffffL);
    }

    public long networkBandwidthRpm() {
        return ((long) syncData.get(5) << 32) | (syncData.get(6) & 0xffffffffL);
    }

    public MemoryNetworkStatus status() {
        int ordinal = Math.max(0, Math.min(syncData.get(7), MemoryNetworkStatus.values().length - 1));
        return MemoryNetworkStatus.values()[ordinal];
    }

    public int cardCount() {
        return syncData.get(8);
    }

    public int pageOffset() {
        return page * SLOTS_PER_PAGE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player != this.player || id < 0 || id > MAX_PAGE) {
            return false;
        }
        page = id;
        broadcastChanges();
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
        private static final Container EMPTY = new SimpleContainer(MemoryDriveBlockEntity.TOTAL_SLOTS);
        private final MemoryDriveMenu menu;
        private final int pageSlot;

        private PagedDriveSlot(MemoryDriveMenu menu, int pageSlot, int x, int y) {
            super(EMPTY, pageSlot, x, y);
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
            return inventory().getStackInSlot(inventorySlot());
        }

        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty() && !mayPlace(stack)) {
                return;
            }
            inventory().setItemDirect(inventorySlot(), stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            menu.drive.setChanged();
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
