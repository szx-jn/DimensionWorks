package dev.szx.dimensionworks.cavefactory.twilight;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartOwnerResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TwilightWorkstationBlockEntity extends KineticBlockEntity implements MenuProvider {
    public enum Status { READY, WRONG_DIMENSION, NO_POWER, REDSTONE, NO_BED, NO_RECIPE, NO_WATER,
        OUTPUT_FULL, NO_BOARS, QUOTA }
    private static final int BOARS = 2;
    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? stack.is(TwilightIndustry.LEAVES)
                : slot == 1 && (biological() ? stack.is(TwilightIndustry.FEED) : stack.is(TwilightIndustry.HUMUS.get()));
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidTank water = new FluidTank(10000, stack -> stack.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final FluidTank output = new FluidTank(10000) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IItemHandler> allItems = LazyOptional.of(() -> items);
    private LazyOptional<IItemHandler> itemInputs = LazyOptional.of(() -> new RangedWrapper(items, 0, 2));
    private LazyOptional<IItemHandler> itemOutputs = LazyOptional.of(() -> new RangedWrapper(items, 2, 3));
    private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);
    private LazyOptional<IFluidHandler> outputCap = LazyOptional.of(() -> new OutputTank());
    private LazyOptional<IFluidHandler> allFluids = LazyOptional.of(() -> new CombinedTankWrapper(water, new OutputTank()));
    private Status status = Status.NO_RECIPE;
    private double progress;
    private int elapsedTicks;
    private int duration = 200;
    private TwilightWorkRecipe activeRecipe;
    private String restoredRecipe = "";
    private final List<UUID> workers = new ArrayList<>();

    public TwilightWorkstationBlockEntity(BlockPos pos, BlockState state) {
        super(TwilightIndustry.BLOCK_ENTITY.get(), pos, state);
    }
    public boolean biological() { return ((TwilightWorkstationBlock) getBlockState().getBlock()).biological(); }
    public boolean ownsAnimal(UUID id) { return workers.contains(id); }
    public IItemHandler inventory() { return items; }
    public FluidTank waterTank() { return water; }
    public FluidTank outputTank() { return output; }
    public Status status() { return status; }
    public int progress() { return (int) progress; }
    public int duration() { return duration; }
    public IFluidHandler manualTank() { return output.isEmpty() ? water : new OutputTank(); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public float calculateStressApplied() { return lastStressApplied = 4.0F; }

    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || ++elapsedTicks < 20) return;
        elapsedTicks = 0;
        process(server);
    }

    private void process(ServerLevel server) {
        if (!level.dimension().location().equals(TwilightIndustry.DIMENSION)) { stop(Status.WRONG_DIMENSION); return; }
        if (isOverStressed() || Math.abs(getSpeed()) < 32) { stop(Status.NO_POWER); return; }
        if (level.hasNeighborSignal(worldPosition)) { stop(Status.REDSTONE); return; }
        if (!biological() && !validBed()) { stop(Status.NO_BED); return; }
        TwilightWorkRecipe recipe = server.getRecipeManager().getAllRecipesFor(TwilightIndustry.RECIPE_TYPE.get())
            .stream().filter(r -> r.biological() == biological()
                && r.accepts(items.getStackInSlot(0), items.getStackInSlot(1))).findFirst().orElse(null);
        if (recipe == null) { stop(Status.NO_RECIPE); return; }
        if (activeRecipe != recipe) {
            if (!recipe.id().toString().equals(restoredRecipe)) { progress = 0; releaseWorkers(0); }
            restoredRecipe = "";
            activeRecipe = recipe;
        }
        duration = recipe.duration();
        if (water.getFluidAmount() < recipe.water()) { stop(Status.NO_WATER); return; }
        if (!hasOutputSpace(recipe)) { stop(Status.OUTPUT_FULL); return; }
        if (biological() && !ensureWorkers(server)) { stop(Status.NO_BOARS); return; }
        if (progress < duration) setStatus(Status.READY);
        // Cap at 128 RPM: animal/environment work cannot be multiplied by stacking RPM.
        progress = Math.min(duration, progress + 20.0 * Math.min(1.0, Math.abs(getSpeed()) / 128.0));
        setChanged();
        if (progress < duration) return;
        ItemStack quotaItem = biological() ? recipe.result() : recipe.fluidResult().getFluid().getBucket()
            .getDefaultInstance();
        if (!FinalProductionGate.tryConsumeItem(GearHeartOwnerResolver.owner(this), quotaItem,
            biological() ? recipe.result().getCount() : Math.max(1, (recipe.fluidResult().getAmount() + 999) / 1000),
            level.getGameTime())) { setStatus(Status.QUOTA); return; }
        // All matching, environment, worker and capacity checks precede every input mutation.
        items.extractItem(0, recipe.firstCount(), false);
        items.extractItem(1, recipe.secondCount(), false);
        water.drain(recipe.water(), IFluidHandler.FluidAction.EXECUTE);
        if (biological()) {
            ItemStack result = recipe.result().copy();
            if (!items.getStackInSlot(2).isEmpty()) result.grow(items.getStackInSlot(2).getCount());
            items.setStackInSlot(2, result);
            releaseWorkers(200);
        } else output.fill(recipe.fluidResult(), IFluidHandler.FluidAction.EXECUTE);
        progress = 0;
        setChanged();
    }

    public boolean validBed() {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos p = worldPosition.offset(x, -1, z);
            if (!level.hasChunkAt(p) || !level.getBlockState(p).is(TwilightIndustry.ROOT_BED)) return false;
        }
        return true;
    }

    private boolean hasOutputSpace(TwilightWorkRecipe r) {
        if (!biological()) return output.fill(r.fluidResult(), IFluidHandler.FluidAction.SIMULATE) == r.fluidResult().getAmount();
        ItemStack stored = items.getStackInSlot(2);
        return stored.isEmpty() || ItemStack.isSameItemSameTags(stored, r.result())
            && stored.getCount() + r.result().getCount() <= stored.getMaxStackSize();
    }

    private AABB workArea() { return new AABB(worldPosition).inflate(3.0, 1.0, 3.0); }
    private boolean ensureWorkers(ServerLevel server) {
        if (!workers.isEmpty()) {
            boolean valid = workers.size() == BOARS && workers.stream().allMatch(id ->
                server.getEntity(id) instanceof Animal animal && TwilightBoarWork.isBoar(animal)
                    && animal.getBoundingBox().intersects(workArea()) && TwilightBoarWork.belongsTo(animal, this));
            if (valid) return true;
            releaseWorkers(0);
            progress = 0;
        }
        List<Animal> available = server.getEntitiesOfClass(Animal.class, workArea(), TwilightBoarWork::available);
        if (available.size() < BOARS) return false;
        for (int i = 0; i < BOARS; i++) {
            Animal animal = available.get(i);
            workers.add(animal.getUUID());
            TwilightBoarWork.claim(animal, this);
        }
        setChanged();
        return true;
    }

    public void releaseWorkers(int cooldown) {
        if (level instanceof ServerLevel server) for (UUID id : workers)
            if (server.getEntity(id) instanceof Animal animal) TwilightBoarWork.release(animal, this, cooldown);
        workers.clear();
    }
    private void stop(Status reason) {
        if (biological()) { releaseWorkers(0); progress = 0; }
        setStatus(reason);
        setChanged();
    }
    private void setStatus(Status value) {
        if (status != value) { status = value; sendData(); }
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return (side == null ? allItems : side == Direction.UP ? itemInputs
                : side == Direction.DOWN ? itemOutputs : LazyOptional.<IItemHandler>empty()).cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER)
            return (side == null ? allFluids : side == Direction.DOWN ? outputCap : waterCap).cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        allItems.invalidate(); itemInputs.invalidate(); itemOutputs.invalidate();
        waterCap.invalidate(); outputCap.invalidate(); allFluids.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        allItems = LazyOptional.of(() -> items);
        itemInputs = LazyOptional.of(() -> new RangedWrapper(items, 0, 2));
        itemOutputs = LazyOptional.of(() -> new RangedWrapper(items, 2, 3));
        waterCap = LazyOptional.of(() -> water); outputCap = LazyOptional.of(() -> new OutputTank());
        allFluids = LazyOptional.of(() -> new CombinedTankWrapper(water, new OutputTank()));
    }

    public CompoundTag portableStorage() {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items.serializeNBT()); tag.put("Water", water.writeToNBT(new CompoundTag()));
        tag.put("Output", output.writeToNBT(new CompoundTag()));
        return tag;
    }
    @Override protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.merge(portableStorage());
        tag.putDouble("Work", progress); tag.putInt("Duration", duration);
        tag.putString("Recipe", activeRecipe == null ? restoredRecipe : activeRecipe.id().toString());
        tag.putInt("WorkStatus", status.ordinal());
        ListTag ids = new ListTag(); workers.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
        tag.put("Workers", ids);
    }
    @Override protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        items.deserializeNBT(tag.getCompound("Items")); water.readFromNBT(tag.getCompound("Water"));
        output.readFromNBT(tag.getCompound("Output"));
        duration = Math.max(20, tag.getInt("Duration"));
        progress = Math.max(0, Math.min(duration, tag.getDouble("Work")));
        restoredRecipe = tag.getString("Recipe"); activeRecipe = null;
        status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, tag.getInt("WorkStatus")))];
        workers.clear();
        for (Tag id : tag.getList("Workers", Tag.TAG_STRING)) workers.add(UUID.fromString(id.getAsString()));
    }

    public final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 9; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> status.ordinal(); case 1 -> water.getFluidAmount(); case 2 -> output.getFluidAmount();
                case 3 -> progress() & 65535; case 4 -> progress() >>> 16;
                case 5 -> duration & 65535; case 6 -> duration >>> 16;
                case 7 -> workers.size(); case 8 -> Math.min(32767, Math.round(Math.abs(getSpeed())));
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
    };
    @Override public Component getDisplayName() {
        return Component.translatable(biological() ? "block.dimensionworks_cave_factory.boar_conditioner"
            : "block.dimensionworks_cave_factory.forest_leacher");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TwilightWorkstationMenu(id, inv, this);
    }

    private final class OutputTank implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return output.getFluid(); }
        @Override public int getTankCapacity(int tank) { return output.getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return output.drain(stack, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return output.drain(amount, action); }
    }
}
