package dev.szx.dimensionworks.mekstress.blockentity;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import dev.szx.dimensionworks.mekstress.memory.MatterBallPacking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Accepts a Matter Ball and exposes its contents to ordinary item and fluid automation. */
public final class MatterBallUnpackerBlockEntity extends BlockEntity {
    private static final int ITEM_OUTPUTS = 27;
    private static final int FLUID_LIMIT = 16_000;
    private final NonNullList<ItemStack> items = NonNullList.withSize(ITEM_OUTPUTS, ItemStack.EMPTY);
    private final List<FluidStack> fluids = new ArrayList<>();
    private int fluidAmount;
    private final IItemHandler itemHandler = new ItemHandler();
    private final IFluidHandler fluidHandler = new FluidHandler();
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemHandler);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidHandler);

    public MatterBallUnpackerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean unpack(ItemStack ball) {
        if (!ball.is(DimensionWorksMekStress.MATTER_BALL.get()) || !ball.hasTag()
            || !ball.getTag().contains("MatterBall", Tag.TAG_COMPOUND)
            || ChannelStockSavedData.encodedSize(ball.save(new CompoundTag()))
                > dev.szx.dimensionworks.mekstress.MekStressConfig.maximumMatterBallNbtBytes()) return false;
        CompoundTag payload = ball.getTag().getCompound("MatterBall");
        if (payload.getInt("Version") != 1) return false;
        ListTag itemTags = payload.getList("Items", Tag.TAG_COMPOUND);
        ListTag fluidTags = payload.getList("Fluids", Tag.TAG_COMPOUND);
        List<ItemStack> parsedItems = new ArrayList<>();
        List<FluidStack> parsedFluids = new ArrayList<>();
        int parsedFluidAmount = 0;
        for (int i = 0; i < itemTags.size(); i++) {
            CompoundTag entry = itemTags.getCompound(i);
            if (!entry.contains("Stack", Tag.TAG_COMPOUND)) return false;
            ItemStack prototype = ItemStack.of(entry.getCompound("Stack"));
            int amount = entry.getInt("Amount");
            int maximum = entry.getInt("Maximum");
            if (prototype.isEmpty() || amount <= 0 || maximum <= 0 || amount > maximum
                || parsedItems.size() >= ITEM_OUTPUTS) return false;
            parsedItems.add(prototype.copyWithCount(amount));
        }
        for (int i = 0; i < fluidTags.size(); i++) {
            CompoundTag entry = fluidTags.getCompound(i);
            if (!entry.contains("Stack", Tag.TAG_COMPOUND)) return false;
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(entry.getCompound("Stack"));
            if (fluid.isEmpty() || fluid.getAmount() <= 0 || parsedFluids.size() >= 27) return false;
            if (fluid.getAmount() > FLUID_LIMIT - parsedFluidAmount) return false;
            parsedFluidAmount += fluid.getAmount();
            parsedFluids.add(fluid);
        }
        if (!isEmpty()) return false;
        for (int i = 0; i < parsedItems.size(); i++) items.set(i, parsedItems.get(i));
        fluids.addAll(parsedFluids);
        fluidAmount = parsedFluidAmount;
        setChanged();
        return true;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && fluids.isEmpty();
    }

    public List<ItemStack> packContents() {
        List<ChannelStockSavedData.ItemSnapshot> itemSnapshot = items.stream().filter(stack -> !stack.isEmpty())
            .map(stack -> new ChannelStockSavedData.ItemSnapshot(stack.copyWithCount(1), "dimensionworks:unpacker",
                stack.getCount(), stack.getMaxStackSize())).toList();
        List<ChannelStockSavedData.FluidSnapshot> fluidSnapshot = fluids.stream()
            .map(stack -> new ChannelStockSavedData.FluidSnapshot(stack.copy(), "dimensionworks:unpacker")).toList();
        return MatterBallPacking.pack(new ChannelStockSavedData.StockSnapshot(itemSnapshot, fluidSnapshot, fluidAmount, 0, 0));
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < ITEM_OUTPUTS; i++) items.set(i, ItemStack.EMPTY);
        fluids.clear();
        fluidAmount = 0;
        ListTag itemTags = tag.getList("OutputItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(itemTags.size(), ITEM_OUTPUTS); i++) {
            CompoundTag node = itemTags.getCompound(i);
            int slot = node.getInt("Slot");
            if (slot >= 0 && slot < ITEM_OUTPUTS) items.set(slot, ItemStack.of(node.getCompound("Stack")));
        }
        ListTag fluidTags = tag.getList("OutputFluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(fluidTags.size(), 27); i++) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidTags.getCompound(i));
            if (!fluid.isEmpty() && fluid.getAmount() > 0 && fluidAmount <= FLUID_LIMIT - fluid.getAmount()) {
                fluids.add(fluid);
                fluidAmount += fluid.getAmount();
            }
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag itemTags = new ListTag();
        for (int i = 0; i < ITEM_OUTPUTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Stack", stack.save(new CompoundTag()));
            itemTags.add(entry);
        }
        tag.put("OutputItems", itemTags);
        ListTag fluidTags = new ListTag();
        for (FluidStack stack : fluids) fluidTags.add(stack.writeToNBT(new CompoundTag()));
        tag.put("OutputFluids", fluidTags);
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> itemHandler);
        fluidCapability = LazyOptional.of(() -> fluidHandler);
    }

    private final class ItemHandler implements IItemHandler {
        @Override public int getSlots() { return ITEM_OUTPUTS + 1; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot > 0 && slot <= ITEM_OUTPUTS ? items.get(slot - 1).copy() : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !stack.is(DimensionWorksMekStress.MATTER_BALL.get())) return stack;
            if (simulate) return MatterBallUnpackerBlockEntity.this.isEmpty() ? ItemStack.EMPTY : stack;
            if (!unpack(stack)) return stack;
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot <= 0 || slot > ITEM_OUTPUTS || amount <= 0) return ItemStack.EMPTY;
            int outputSlot = slot - 1;
            ItemStack current = items.get(outputSlot);
            if (current.isEmpty()) return ItemStack.EMPTY;
            int removed = Math.min(amount, current.getCount());
            ItemStack result = current.copyWithCount(removed);
            if (!simulate) {
                current.shrink(removed);
                if (current.isEmpty()) items.set(outputSlot, ItemStack.EMPTY);
                setChanged();
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(DimensionWorksMekStress.MATTER_BALL.get());
        }
    }

    private final class FluidHandler implements IFluidHandler {
        @Override public int getTanks() { return fluids.size(); }
        @Override public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < fluids.size() ? fluids.get(tank).copy() : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) { return FLUID_LIMIT; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            for (int i = 0; i < fluids.size(); i++) {
                FluidStack current = fluids.get(i);
                if (!current.isFluidEqual(resource) || !FluidStack.areFluidStackTagsEqual(current, resource)) continue;
                return drainAt(i, resource.getAmount(), action.simulate());
            }
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return maxDrain <= 0 || fluids.isEmpty() ? FluidStack.EMPTY : drainAt(0, maxDrain, action.simulate());
        }
        private FluidStack drainAt(int index, int requested, boolean simulate) {
            FluidStack current = fluids.get(index);
            int drained = Math.min(requested, current.getAmount());
            FluidStack result = new FluidStack(current, drained);
            if (!simulate) {
                current.shrink(drained);
                fluidAmount -= drained;
                if (current.isEmpty()) fluids.remove(index);
                setChanged();
            }
            return result;
        }
    }
}
