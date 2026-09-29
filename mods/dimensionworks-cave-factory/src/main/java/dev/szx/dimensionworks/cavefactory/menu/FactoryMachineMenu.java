package dev.szx.dimensionworks.cavefactory.menu;

import dev.szx.dimensionworks.cavefactory.blockentity.FactoryMachineBlockEntity;
import dev.szx.dimensionworks.cavefactory.item.ModuleItem;
import dev.szx.dimensionworks.cavefactory.logic.MachineInventory;
import dev.szx.dimensionworks.cavefactory.registry.CFMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FactoryMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_START = 0;
    private static final int MACHINE_SLOT_END = MachineInventory.MENU_SLOTS;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_END;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final FactoryMachineBlockEntity machine;

    public FactoryMachineMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readMachine(inventory, buffer));
    }

    public FactoryMachineMenu(int id, Inventory inventory, FactoryMachineBlockEntity machine) {
        super(CFMenus.factoryMachine(), id);
        if (machine == null) {
            throw new IllegalArgumentException("Factory machine is not loaded");
        }
        this.machine = machine;

        addMachineSlots(inventory.player);
        addPlayerInventory(inventory);
        addDataSlots(machine.containerData());
    }

    private void addMachineSlots(Player player) {
        for (int i = 0; i < 4; i++) {
            addSlot(new SlotItemHandler(machine.inputItems(), i, 8 + i * 20, 20));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new OutputSlot(machine.outputItems(), i, 8 + i * 20, 48));
        }
        addSlot(new OutputSlot(machine.residues(), 0, 8, 76));
        addSlot(new AmpouleInputSlot(machine.ampouleInput(), 0, 38, 76));
        addSlot(new OutputSlot(machine.ampouleOutput(), 0, 68, 76));
        addSlot(new ModuleSlot(machine, player, 0, 150, 20));
        addSlot(new ModuleSlot(machine, player, 1, 150, 48));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 104 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 162));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ModuleItem) {
            if (!moveItemStackTo(stack, MachineInventory.NUMERIC_MODULE_SLOT,
                MachineInventory.MECHANISM_MODULE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem) {
            if (!moveItemStackTo(stack, MachineInventory.EMPTY_AMPOULE_SLOT,
                MachineInventory.EMPTY_AMPOULE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, MachineInventory.ITEM_INPUT_START,
            MachineInventory.ITEM_OUTPUT_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.isMachineValid()
            && player.distanceToSqr(
                machine.machinePos().getX() + 0.5D,
                machine.machinePos().getY() + 0.5D,
                machine.machinePos().getZ() + 0.5D
            ) <= 64.0D;
    }

    public FactoryMachineBlockEntity machine() {
        return machine;
    }

    public ContainerData data() {
        return machine.containerData();
    }

    private static FactoryMachineBlockEntity readMachine(Inventory inventory, FriendlyByteBuf buffer) {
        var level = inventory.player.level();
        if (level == null) {
            return null;
        }
        var pos = buffer.readBlockPos();
        if (level.getBlockEntity(pos) instanceof FactoryMachineBlockEntity machine) {
            return machine;
        }
        return null;
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class AmpouleInputSlot extends SlotItemHandler {
        private AmpouleInputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem ampoule
                && !ampoule.hasStoredFluid(stack);
        }
    }

    private static final class ModuleSlot extends SlotItemHandler {
        private final FactoryMachineBlockEntity machine;
        private final Player player;

        private ModuleSlot(FactoryMachineBlockEntity machine, Player player, int index, int x, int y) {
            super(machine.moduleInventory(), index, x, y);
            this.machine = machine;
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return machine.isOwner(player) && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return machine.isOwner(player);
        }
    }
}
