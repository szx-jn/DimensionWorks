package dev.szx.dimensionworks.cavefactory.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import dev.szx.dimensionworks.cavefactory.item.ModuleItem;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.MachineInventory;
import dev.szx.dimensionworks.cavefactory.logic.MachineTuning;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import dev.szx.dimensionworks.cavefactory.logic.PhaseCycle;
import dev.szx.dimensionworks.cavefactory.logic.PortMode;
import dev.szx.dimensionworks.rpmlimit.api.RpmLimitServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class FactoryMachineBlockEntity extends KineticBlockEntity
    implements MachineInventory, ModuleHost, net.minecraft.world.MenuProvider {
    private final ItemStackHandler inputItems = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            onStorageContentsChanged();
        }
    };
    private final ItemStackHandler outputItems = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onStorageContentsChanged();
        }
    };
    private final ItemStackHandler residue = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onStorageContentsChanged();
        }
    };
    private final ItemStackHandler ampouleInput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FluidAmpouleItem ampoule && !ampoule.hasStoredFluid(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onStorageContentsChanged();
        }
    };
    private final ItemStackHandler ampouleOutput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onStorageContentsChanged();
        }
    };
    private final ItemStackHandler modules = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof ModuleItem moduleItem)) {
                return false;
            }
            return acceptsModule(moduleItem.module(), slot == 1);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onModuleSlotsChanged();
        }
    };
    private final FluidTank inputTank = new FluidTank(4_000) {
        @Override
        protected void onContentsChanged() {
            onStorageContentsChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(8_000) {
        @Override
        protected void onContentsChanged() {
            onStorageContentsChanged();
        }
    };
    private final IItemHandler outputItemCapability = new FactoryOutputItemHandler(outputItems, residue, ampouleOutput);

    private final MachinePortController portController = new MachinePortController();
    private UUID owner;
    private String ownerName = "";
    private PhaseCycle phaseCycle;
    private boolean redstoneLocked;
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> phaseCycle.phase().ordinal();
                case 1 -> phaseCycle.progress();
                case 2 -> CaveFactoryConfig.PHASE_BATCHES.get();
                case 3 -> Math.round(Math.abs(effectiveRpm()));
                case 4 -> structureValid() ? 1 : 0;
                case 5 -> redstoneLocked ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    protected FactoryMachineBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState state
    ) {
        super(type, pos, state);
        phaseCycle = new PhaseCycle(FactoryPhase.PHASE_A, 0, CaveFactoryConfig.PHASE_BATCHES.get());
        inputTank.setValidator(dev.szx.dimensionworks.cavefactory.registry.CFFluids::isFactoryFluid);
        outputTank.setValidator(dev.szx.dimensionworks.cavefactory.registry.CFFluids::isFactoryFluid);
        setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!hasPotentialWork() && level.getGameTime() % 10 != 0) {
            return;
        }
        redstoneLocked = level.hasNeighborSignal(worldPosition);
        expireAmpoulesInternal();
        processServerTick();
    }

    protected void onStorageContentsChanged() {
        setChanged();
        sendData();
    }

    protected abstract void processServerTick();

    protected boolean hasPotentialWork() {
        return !inputTank.isEmpty()
            || !getStack(RESIDUE_SLOT).isEmpty()
            || !getStack(EMPTY_AMPOULE_SLOT).isEmpty()
            || !getStack(FULL_AMPOULE_SLOT).isEmpty()
            || !inputItems.getStackInSlot(0).isEmpty()
            || !inputItems.getStackInSlot(1).isEmpty()
            || !inputItems.getStackInSlot(2).isEmpty()
            || !inputItems.getStackInSlot(3).isEmpty();
    }

    public boolean hasEffectiveSpeed() {
        return !isOverStressed() && Math.abs(effectiveRpm()) > 0.01F;
    }

    public float effectiveRpm() {
        return RpmLimitServices.rpm().clamp(this, getTheoreticalSpeed());
    }

    public double overspeedMultiplier() {
        var overspeed = RpmLimitServices.overspeed();
        if (!overspeed.isEnabled()) return 1.0D;
        return overspeed.multiplier(effectiveRpm(), CaveFactoryConfig.KINETIC_SATURATION_RPM.get());
    }

    public float stressImpactWithOverspeed(float baseImpact) {
        float impact = Math.max(0, baseImpact);
        var overspeed = RpmLimitServices.overspeed();
        if (!overspeed.isStressScalingEnabled() || overspeed.isWhitelisted(this)) return impact;
        return (float) (impact * overspeedMultiplier());
    }

    public int batchesThisTick() {
        return RpmLimitServices.overspeed().batches(overspeedMultiplier(), level == null ? 0 : level.getGameTime());
    }

    protected void recordSuccessfulBatch() {
        phaseCycle.recordSuccess(redstoneLocked);
        setChanged();
    }

    public boolean redstoneLocked() {
        return redstoneLocked;
    }

    public FactoryPhase phase() {
        return phaseCycle.phase();
    }

    public int phaseProgress() {
        return phaseCycle.progress();
    }

    public void setPhaseState(FactoryPhase phase, int progress) {
        phaseCycle = PhaseCycle.fromState(phase, progress, CaveFactoryConfig.PHASE_BATCHES.get());
    }

    public PortMode portMode(Direction direction) {
        return portController.mode(direction);
    }

    public boolean cyclePort(Direction direction, Player player) {
        if (!isOwner(player)) {
            return false;
        }
        portController.cycle(direction, level, worldPosition);
        refreshKineticConnection(direction);
        setChanged();
        return true;
    }

    private static final class FactoryOutputItemHandler implements IItemHandler {
        private final ItemStackHandler outputs;
        private final ItemStackHandler residues;
        private final ItemStackHandler ampoules;

        private FactoryOutputItemHandler(
            ItemStackHandler outputs,
            ItemStackHandler residues,
            ItemStackHandler ampoules
        ) {
            this.outputs = outputs;
            this.residues = residues;
            this.ampoules = ampoules;
        }

        @Override
        public int getSlots() {
            return outputs.getSlots() + residues.getSlots() + ampoules.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return target(slot).getStackInSlot(localSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return target(slot).extractItem(localSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return target(slot).getSlotLimit(localSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        private ItemStackHandler target(int slot) {
            int outputSlots = outputs.getSlots();
            int residueSlots = residues.getSlots();
            if (slot < outputSlots) {
                return outputs;
            }
            if (slot < outputSlots + residueSlots) {
                return residues;
            }
            if (slot < getSlots()) {
                return ampoules;
            }
            throw new IndexOutOfBoundsException("Slot " + slot);
        }

        private int localSlot(int slot) {
            int outputSlots = outputs.getSlots();
            int residueSlots = residues.getSlots();
            if (slot < outputSlots) {
                return slot;
            }
            if (slot < outputSlots + residueSlots) {
                return slot - outputSlots;
            }
            if (slot < getSlots()) {
                return slot - outputSlots - residueSlots;
            }
            throw new IndexOutOfBoundsException("Slot " + slot);
        }
    }

    public Direction kineticPort() {
        return portController.kineticPort();
    }

    protected void refreshKineticConnection(Direction changedDirection) {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction kineticSide = kineticPort();
        if (kineticSide == null) {
            removeSource();
            return;
        }
        BlockEntity portEntity = level.getBlockEntity(worldPosition.relative(kineticSide));
        if (!(portEntity instanceof FactoryPortBlockEntity port)) {
            removeSource();
            return;
        }
        setNetwork(port.network);
        source = port.getBlockPos();
        speed = port.getSpeed();
        networkDirty = true;
        setChanged();
    }

    public void onKineticPortSpeedChanged() {
        refreshKineticConnection(null);
    }

    public void setOwner(Player player) {
        if (owner != null) {
            return;
        }
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return owner == null || (player != null && owner.equals(player.getUUID()));
    }

    public UUID owner() {
        return owner;
    }

    public Component ownerName() {
        return ownerName.isBlank() ? Component.translatable("gui.dimensionworks_cave_factory.unowned")
            : Component.literal(ownerName);
    }

    public void expireAmpoulesInternal() {
        long now = System.currentTimeMillis();
        if (MachineAmpouleHandler.expireHandler(ampouleInput, level, now, this::addResidueOrDrop)) {
            setChanged();
        }
        if (MachineAmpouleHandler.expireHandler(ampouleOutput, level, now, this::addResidueOrDrop)) {
            setChanged();
        }
    }

    public void addResidueOrDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remainder = insertInternal(residue, 0, stack.copy(), false);
        dropRemainder(remainder);
    }

    protected static ItemStack insertInternal(ItemStackHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = insertInternal(handler, slot, remainder, simulate);
        }
        return remainder;
    }

    protected static ItemStack insertInternal(
        ItemStackHandler handler,
        int slot,
        ItemStack stack,
        boolean simulate
    ) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = handler.getStackInSlot(slot);
        int slotLimit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
        if (existing.isEmpty()) {
            int moved = dev.szx.dimensionworks.cavefactory.logic.InternalInsertionMath.intoEmptySlot(
                stack.getCount(),
                slotLimit
            );
            if (!simulate && moved > 0) {
                handler.setStackInSlot(slot, stack.copyWithCount(moved));
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int limit = Math.min(handler.getSlotLimit(slot), existing.getMaxStackSize());
        int moved = dev.szx.dimensionworks.cavefactory.logic.InternalInsertionMath.intoExistingSlot(
            existing.getCount(),
            stack.getCount(),
            limit
        );
        if (!simulate && moved > 0) {
            handler.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + moved));
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    protected void dropRemainder(ItemStack remainder) {
        if (remainder.isEmpty() || level == null) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
            level,
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 0.5D,
            worldPosition.getZ() + 0.5D,
            remainder
        ));
    }

    public void addOrDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remainder = stack.copy();
        remainder = insertInternal(outputItems, remainder, false);
        if (!remainder.isEmpty()) {
            remainder = insertInternal(residue, 0, remainder, false);
        }
        dropRemainder(remainder);
    }

    @Override
    public boolean ejectModules() {
        if (level == null) {
            return false;
        }
        Player ownerPlayer = null;
        if (owner != null && level instanceof ServerLevel serverLevel) {
            ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(owner);
        }
        boolean ejected = false;
        for (int slot = 0; slot < modules.getSlots(); slot++) {
            ItemStack stack = modules.extractItem(slot, 64, false);
            if (!stack.isEmpty()) {
                ejected = true;
                if (ownerPlayer != null && ownerPlayer.getInventory().add(stack)) {
                    continue;
                }
                level.addFreshEntity(new ItemEntity(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    stack
                ));
            }
        }
        return ejected;
    }

    public void dropContents() {
        for (int slot = 0; slot < NUMERIC_MODULE_SLOT; slot++) {
            ItemStack stack = getStack(slot);
            if (!stack.isEmpty()) {
                setStack(slot, ItemStack.EMPTY);
                if (level != null) {
                    level.addFreshEntity(new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D,
                        stack
                    ));
                }
            }
        }
        int storedFluid = inputTank.getFluidAmount() + outputTank.getFluidAmount();
        if (level instanceof ServerLevel serverLevel && storedFluid > 0) {
            int residueCount = Math.max(1, storedFluid / 1_000);
            ItemStack residueStack = new ItemStack(dev.szx.dimensionworks.cavefactory.registry.CFItems.residue(
                machineTypeForResidue()
            ).get(), residueCount);
            serverLevel.addFreshEntity(new ItemEntity(
                serverLevel,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                residueStack
            ));
        }
        inputTank.setFluid(FluidStack.EMPTY);
        outputTank.setFluid(FluidStack.EMPTY);
    }

    public abstract dev.szx.dimensionworks.cavefactory.logic.MachineType machineTypeForResidue();

    protected void onModuleSlotsChanged() {
        refreshFluidCapacity();
        setChanged();
        sendData();
    }

    protected void refreshFluidCapacity() {
        inputTank.setCapacity(MachineTuning.scaleFluidCapacity(
            MachineTuning.BASE_FLUID_CAPACITY,
            hasFluidCapacityModule()
        ));
        outputTank.setCapacity(MachineTuning.scaleFluidCapacity(
            MachineTuning.BASE_FLUID_CAPACITY * 2,
            hasFluidCapacityModule()
        ));
    }

    protected boolean hasFluidCapacityModule() {
        return moduleAt(0) == FactoryModule.PRESSURE_BUFFER;
    }

    @Override
    public ItemStackHandler moduleInventory() {
        return modules;
    }

    @Override
    public boolean hasRecipeMapping(FactoryModule module) {
        if (module != FactoryModule.PHASE_CONVERTER || level == null) {
            return module != FactoryModule.PHASE_CONVERTER;
        }
        for (var rule : level.getRecipeManager().getAllRecipesFor(
            dev.szx.dimensionworks.cavefactory.registry.CFRecipes.MODULE_RECIPE_RULE_TYPE.get()
        )) {
            if (rule.value().module() == module && !rule.value().recipeIds().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MachineCapability machineCapability() {
        return MachineCapability.ITEM_ONLY;
    }

    @Override
    public ItemStack getStack(int slot) {
        return switch (slot) {
            case 0, 1, 2, 3 -> inputItems.getStackInSlot(slot);
            case 4, 5, 6, 7 -> outputItems.getStackInSlot(slot - 4);
            case RESIDUE_SLOT -> residue.getStackInSlot(0);
            case EMPTY_AMPOULE_SLOT -> ampouleInput.getStackInSlot(0);
            case FULL_AMPOULE_SLOT -> ampouleOutput.getStackInSlot(0);
            default -> modules.getStackInSlot(slot - NUMERIC_MODULE_SLOT);
        };
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        switch (slot) {
            case 0, 1, 2, 3 -> inputItems.setStackInSlot(slot, stack);
            case 4, 5, 6, 7 -> outputItems.setStackInSlot(slot - 4, stack);
            case RESIDUE_SLOT -> residue.setStackInSlot(0, stack);
            case EMPTY_AMPOULE_SLOT -> ampouleInput.setStackInSlot(0, stack);
            case FULL_AMPOULE_SLOT -> ampouleOutput.setStackInSlot(0, stack);
            case NUMERIC_MODULE_SLOT, MECHANISM_MODULE_SLOT -> modules.setStackInSlot(slot - NUMERIC_MODULE_SLOT, stack);
            default -> throw new IllegalArgumentException("Unknown slot " + slot);
        }
        setChanged();
    }

    public ItemStackHandler inputItems() {
        return inputItems;
    }

    public ItemStackHandler outputItems() {
        return outputItems;
    }

    public ItemStackHandler residues() {
        return residue;
    }

    public ItemStackHandler ampouleInput() {
        return ampouleInput;
    }

    public ItemStackHandler ampouleOutput() {
        return ampouleOutput;
    }

    @Override
    public int fluidInputCapacity() {
        return inputTank.getCapacity();
    }

    @Override
    public int fluidOutputCapacity() {
        return outputTank.getCapacity();
    }

    @Override
    public FluidStack fluidInput() {
        return inputTank.getFluid();
    }

    @Override
    public FluidStack fluidOutput() {
        return outputTank.getFluid();
    }

    public FluidTank inputTank() {
        return inputTank;
    }

    public FluidTank outputTank() {
        return outputTank;
    }

    @Override
    public BlockPos machinePos() {
        return worldPosition;
    }

    @Override
    public boolean isMachineValid() {
        return level != null && !isRemoved();
    }

    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        return portController.itemHandler(side, isRemoved(), inputItems, outputItemCapability);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return portController.fluidHandler(side, isRemoved(), inputTank, outputTank);
    }

    @Override
    protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        MachinePersistenceCodec.write(
            tag,
            registries,
            owner,
            ownerName,
            phaseCycle,
            inputItems,
            outputItems,
            residue,
            ampouleInput,
            ampouleOutput,
            modules,
            inputTank,
            outputTank,
            portController
        );
    }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        MachinePersistenceCodec.MachineState state = MachinePersistenceCodec.readState(
            tag,
            CaveFactoryConfig.PHASE_BATCHES.get()
        );
        owner = state.owner();
        ownerName = state.ownerName();
        phaseCycle = state.phaseCycle();
        MachinePersistenceCodec.readStorage(
            tag,
            registries,
            inputItems,
            outputItems,
            residue,
            ampouleInput,
            ampouleOutput,
            modules,
            inputTank,
            outputTank,
            this::refreshFluidCapacity,
            portController
        );
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return writeClient(new CompoundTag(), registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void destroy() {
        ejectModules();
        super.destroy();
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new dev.szx.dimensionworks.cavefactory.menu.FactoryMachineMenu(id, inventory, this);
    }

    public ContainerData containerData() {
        return containerData;
    }

    public boolean hasStoredFluid() {
        if (inputTank.getFluidAmount() > 0 || outputTank.getFluidAmount() > 0) {
            return true;
        }
        return MachineAmpouleHandler.hasStoredFluid(ampouleInput, ampouleOutput);
    }

    public void onRemovedByBlock() {
        dropContents();
    }

    public boolean structureValid() {
        return isMachineValid();
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
        return MachineGogglePresenter.appendTooltip(
            tooltip, effectiveRpm(), overspeedMultiplier(), phaseCycle, redstoneLocked);
    }
}
