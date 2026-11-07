package dev.szx.dimensionworks.cavefactory.twilight;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class TwilightWorkstationMenu extends AbstractContainerMenu {
    public final TwilightWorkstationBlockEntity machine;
    private final ContainerData data;
    public TwilightWorkstationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (TwilightWorkstationBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public TwilightWorkstationMenu(int id, Inventory inventory, TwilightWorkstationBlockEntity machine) {
        super(TwilightIndustry.MENU.get(), id);
        if (machine == null) throw new IllegalArgumentException("Twilight workstation is not loaded");
        this.machine = machine;
        this.data = inventory.player.level().isClientSide ? new SimpleContainerData(9) : machine.data;
        addSlot(new SlotItemHandler(machine.inventory(), 0, 26, 42));
        addSlot(new SlotItemHandler(machine.inventory(), 1, 54, 42));
        addSlot(new SlotItemHandler(machine.inventory(), 2, 128, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 142 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 200));
        addDataSlots(data);
    }
    public int value(int index) { return data.get(index); }
    public int progress() { return data.get(3) & 65535 | data.get(4) << 16; }
    public int duration() { return data.get(5) & 65535 | data.get(6) << 16; }
    @Override public boolean stillValid(Player player) {
        return !machine.isRemoved() && player.level().getBlockEntity(machine.getBlockPos()) == machine
            && player.distanceToSqr(machine.getBlockPos().getX() + .5, machine.getBlockPos().getY() + .5,
                machine.getBlockPos().getZ() + .5) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 3 ? !moveItemStackTo(stack, 3, 39, true) : !moveItemStackTo(stack, 0, 2, false))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
