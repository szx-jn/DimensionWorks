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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public abstract class FactoryMachineBlockEntity extends KineticBlockEntity
    implements MachineInventory, ModuleHost, net.minecraft.world.MenuProvider {
    private static final String OWNER_KEY = "Owner";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final String PHASE_KEY = "FactoryPhase";
    private static final String PHASE_PROGRESS_KEY = "PhaseProgress";
    private static final String INPUT_ITEMS_KEY = "InputItems";
    private static final String OUTPUT_ITEMS_KEY = "OutputItems";
    private static final String RESIDUE_KEY = "Residue";
    private static final String AMPOULE_IN_KEY = "AmpouleInput";
    private static final String AMPOULE_OUT_KEY = "AmpouleOutput";
    private static final String MODULES_KEY = "Modules";
    private static final String INPUT_FLUID_KEY = "InputFluid";
    private static final String OUTPUT_FLUID_KEY = "OutputFluid";
    private static final String PORT_MODES_KEY = "PortModes";

    private final ItemStackHandler inputItems = new ItemStackHandler(4);
    private final ItemStackHandler outputItems = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final ItemStackHandler residue = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final ItemStackHandler ampouleInput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FluidAmpouleItem ampoule && !ampoule.hasStoredFluid(stack);
        }
    };
    private final ItemStackHandler ampouleOutput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
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
    private final FluidTank inputTank = new FluidTank(4_000);
    private final FluidTank outputTank = new FluidTank(8_000);
    private final LazyOptional<IItemHandler> inputItemCapability = LazyOptional.of(() -> inputItems);
    private final LazyOptional<IItemHandler> outputItemCapability = LazyOptional.of(
        () -> new FactoryOutputItemHandler(outputItems, residue, ampouleOutput)
    );
    private final LazyOptional<IFluidHandler> inputFluidCapability = LazyOptional.of(() -> inputTank);
    private final LazyOptional<IFluidHandler> outputFluidCapability = LazyOptional.of(() -> outputTank);

    private final Map<Direction, PortMode> portModes = new EnumMap<>(Direction.class);
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
        for (Direction direction : Direction.values()) {
            portModes.put(direction, PortMode.DISABLED);
        }
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
        try {
            return dev.szx.dimensionworks.rpmlimit.RpmLimitManager.clamp(this, getTheoreticalSpeed());
        } catch (LinkageError | RuntimeException ignored) {
            return getTheoreticalSpeed();
        }
    }

    public double overspeedMultiplier() {
        try {
            if (!dev.szx.dimensionworks.rpmlimit.RpmLimitConfig.OVERSPEED_ENABLED.get()) {
                return 1.0D;
            }
            return dev.szx.dimensionworks.rpmlimit.OverspeedCurve.multiplier(
                effectiveRpm(),
                CaveFactoryConfig.KINETIC_SATURATION_RPM.get(),
                dev.szx.dimensionworks.rpmlimit.RpmLimitConfig.OVERSPEED_CAP.get()
            );
        } catch (LinkageError | RuntimeException ignored) {
            float rpm = effectiveRpm();
            return rpm <= 0 ? 1 : Math.max(1, rpm / CaveFactoryConfig.KINETIC_SATURATION_RPM.get());
        }
    }

    public float stressImpactWithOverspeed(float baseImpact) {
        float impact = Math.max(0, baseImpact);
        try {
            if (!dev.szx.dimensionworks.rpmlimit.RpmLimitConfig.OVERSPEED_STRESS_SCALING.get()
                || dev.szx.dimensionworks.rpmlimit.OverspeedBonus.isWhitelisted(this)) {
                return impact;
            }
        } catch (LinkageError | RuntimeException ignored) {
        }
        return (float) (impact * overspeedMultiplier());
    }

    public int batchesThisTick() {
        try {
            return dev.szx.dimensionworks.rpmlimit.OverspeedCurve.batches(
                overspeedMultiplier(),
                level == null ? 0 : level.getGameTime()
            );
        } catch (LinkageError | RuntimeException ignored) {
            return (int) Math.max(1, overspeedMultiplier());
        }
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
        return portModes.getOrDefault(direction, PortMode.DISABLED);
    }

    public boolean cyclePort(Direction direction, Player player) {
        if (!isOwner(player)) {
            return false;
        }
        PortMode next = portMode(direction).next();
        if (next == PortMode.KINETIC_INPUT && hasKineticPort(direction)) {
            next = next.next();
        }
        portModes.put(direction, next);
        refreshKineticConnection(direction);
        if (level != null) {
            BlockEntity port = level.getBlockEntity(worldPosition.relative(direction));
            if (port != null) {
                port.invalidateCaps();
                level.sendBlockUpdated(port.getBlockPos(), port.getBlockState(), port.getBlockState(), 3);
            }
        }
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

    private boolean hasKineticPort(Direction except) {
        for (Map.Entry<Direction, PortMode> entry : portModes.entrySet()) {
            if (entry.getKey() != except && entry.getValue() == PortMode.KINETIC_INPUT) {
                return true;
            }
        }
        return false;
    }

    public Direction kineticPort() {
        for (Map.Entry<Direction, PortMode> entry : portModes.entrySet()) {
            if (entry.getValue() == PortMode.KINETIC_INPUT) {
                return entry.getKey();
            }
        }
        return null;
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
        expireAmpoules(ampouleInput, now);
        expireAmpoules(ampouleOutput, now);
    }

    private void expireAmpoules(ItemStackHandler handler, long now) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof FluidAmpouleItem ampoule && level != null) {
                AmpouleExpiry.observe(
                    stack,
                    level.dimension().location().toString(),
                    now,
                    CaveFactoryConfig.STABILITY_SECONDS.get() * 1000L
                );
            }
            AmpouleExpiry.Settlement settlement = AmpouleExpiry.settle(stack, now);
            if (settlement.expired()) {
                int count = stack.getCount();
                handler.setStackInSlot(slot, settlement.stack().copyWithCount(count));
                addResidueOrDrop(settlement.residue().copyWithCount(count));
                setChanged();
            }
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
        if (!ItemStack.isSameItemSameTags(existing, stack)) {
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
            if (rule.module() == module && !rule.recipeIds().isEmpty()) {
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

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ModuleHost.CAPABILITY) {
            return moduleCapability.cast();
        }
        if (side != null) {
            PortMode mode = portMode(side);
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                if (mode == PortMode.ITEM_INPUT) {
                    return inputItemCapability.cast();
                }
                if (mode == PortMode.ITEM_OUTPUT) {
                    return outputItemCapability.cast();
                }
            }
            if (capability == ForgeCapabilities.FLUID_HANDLER) {
                if (mode == PortMode.FLUID_INPUT) {
                    return inputFluidCapability.cast();
                }
                if (mode == PortMode.FLUID_OUTPUT) {
                    return outputFluidCapability.cast();
                }
            }
        }
        return super.getCapability(capability, side);
    }

    private final LazyOptional<ModuleHost> moduleCapability = LazyOptional.of(() -> this);

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputItemCapability.invalidate();
        outputItemCapability.invalidate();
        inputFluidCapability.invalidate();
        outputFluidCapability.invalidate();
        moduleCapability.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString(OWNER_KEY, owner == null ? "" : owner.toString());
        tag.putString(OWNER_NAME_KEY, ownerName);
        tag.putString(PHASE_KEY, phaseCycle.phase().name());
        tag.putInt(PHASE_PROGRESS_KEY, phaseCycle.progress());
        tag.put(INPUT_ITEMS_KEY, inputItems.serializeNBT());
        tag.put(OUTPUT_ITEMS_KEY, outputItems.serializeNBT());
        tag.put(RESIDUE_KEY, residue.serializeNBT());
        tag.put(AMPOULE_IN_KEY, ampouleInput.serializeNBT());
        tag.put(AMPOULE_OUT_KEY, ampouleOutput.serializeNBT());
        tag.put(MODULES_KEY, modules.serializeNBT());
        tag.put(INPUT_FLUID_KEY, inputTank.writeToNBT(new CompoundTag()));
        tag.put(OUTPUT_FLUID_KEY, outputTank.writeToNBT(new CompoundTag()));
        CompoundTag modes = new CompoundTag();
        for (Direction direction : Direction.values()) {
            modes.putString(direction.getName(), portMode(direction).id());
        }
        tag.put(PORT_MODES_KEY, modes);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        owner = tag.getString(OWNER_KEY).isBlank() ? null : UUID.fromString(tag.getString(OWNER_KEY));
        ownerName = tag.getString(OWNER_NAME_KEY);
        FactoryPhase phase = FactoryPhase.PHASE_A;
        try {
            phase = FactoryPhase.valueOf(tag.getString(PHASE_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        phaseCycle = PhaseCycle.fromState(phase, tag.getInt(PHASE_PROGRESS_KEY), CaveFactoryConfig.PHASE_BATCHES.get());
        if (tag.contains(INPUT_ITEMS_KEY)) inputItems.deserializeNBT(tag.getCompound(INPUT_ITEMS_KEY));
        if (tag.contains(OUTPUT_ITEMS_KEY)) outputItems.deserializeNBT(tag.getCompound(OUTPUT_ITEMS_KEY));
        if (tag.contains(RESIDUE_KEY)) residue.deserializeNBT(tag.getCompound(RESIDUE_KEY));
        if (tag.contains(AMPOULE_IN_KEY)) ampouleInput.deserializeNBT(tag.getCompound(AMPOULE_IN_KEY));
        if (tag.contains(AMPOULE_OUT_KEY)) ampouleOutput.deserializeNBT(tag.getCompound(AMPOULE_OUT_KEY));
        if (tag.contains(MODULES_KEY)) modules.deserializeNBT(tag.getCompound(MODULES_KEY));
        refreshFluidCapacity();
        if (tag.contains(INPUT_FLUID_KEY)) inputTank.readFromNBT(tag.getCompound(INPUT_FLUID_KEY));
        if (tag.contains(OUTPUT_FLUID_KEY)) outputTank.readFromNBT(tag.getCompound(OUTPUT_FLUID_KEY));
        CompoundTag modes = tag.getCompound(PORT_MODES_KEY);
        for (Direction direction : Direction.values()) {
            String id = modes.getString(direction.getName());
            PortMode mode = PortMode.DISABLED;
            for (PortMode candidate : PortMode.values()) {
                if (candidate.id().equals(id)) {
                    mode = candidate;
                    break;
                }
            }
            portModes.put(direction, mode);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return writeClient(new CompoundTag());
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
        for (ItemStackHandler handler : java.util.List.of(ampouleInput, ampouleOutput)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.getItem() instanceof FluidAmpouleItem ampoule && ampoule.hasStoredFluid(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onRemovedByBlock() {
        dropContents();
    }

    public boolean structureValid() {
        return isMachineValid();
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable(
            "goggles.dimensionworks_cave_factory.status",
            Math.round(Math.abs(effectiveRpm())),
            CaveFactoryConfig.KINETIC_SATURATION_RPM.get(),
            String.format(java.util.Locale.ROOT, "%.2f", overspeedMultiplier())
        ));
        tooltip.add(Component.translatable(
            "goggles.dimensionworks_cave_factory.phase",
            phaseCycle.phase().name(),
            phaseCycle.progress(),
            CaveFactoryConfig.PHASE_BATCHES.get()
        ));
        if (redstoneLocked) {
            tooltip.add(Component.translatable("goggles.dimensionworks_cave_factory.redstone_lock"));
        }
        return true;
    }
}
