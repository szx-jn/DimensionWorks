package dev.szx.dimensionworks.cavefactory.item;

import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.api.IFluidAmpoule;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.logic.AmpouleState;
import dev.szx.dimensionworks.cavefactory.logic.FactoryDimension;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.registry.CFDataComponents;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

public class FluidAmpouleItem extends Item implements IFluidAmpoule {
    public static final int DEFAULT_CAPACITY = 250;
    public static final String FLUID_KEY = "Fluid";
    public static final String ORIGIN_KEY = "OriginDimension";
    public static final String EXPIRY_KEY = "ExpiresAtEpochMs";
    public static final String RESIDUE_KEY = "SourceResidue";

    public FluidAmpouleItem(Properties properties) {
        super(properties);
    }

    public static FluidStack storedFluid(ItemStack stack) {
        AmpouleFluid value = stack.get(CFDataComponents.AMPOULE_FLUID.get());
        if (value != null) {
            return value.toStack();
        }
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (customData == null || !customData.contains(FLUID_KEY, Tag.TAG_COMPOUND)) {
            return FluidStack.EMPTY;
        }
        CompoundTag legacy = customData.getCompound(FLUID_KEY);
        if (!legacy.contains("FluidName", Tag.TAG_STRING) || !legacy.contains("Amount", Tag.TAG_INT)) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(legacy.getString("FluidName"));
        if (id == null) {
            return FluidStack.EMPTY;
        }
        var fluid = BuiltInRegistries.FLUID.getHolder(id).orElse(null);
        int amount = legacy.getInt("Amount");
        if (fluid == null || amount <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack result = new FluidStack(fluid, amount);
        CompoundTag legacyComponents = legacy.getCompound("Tag");
        if (!legacyComponents.isEmpty()) {
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyComponents));
        }
        return result;
    }

    public AmpouleState readState(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null) {
            return AmpouleState.fresh("");
        }
        String origin = tag.getString(ORIGIN_KEY);
        long expiry = tag.getLong(EXPIRY_KEY);
        return new AmpouleState(origin, expiry);
    }

    public void writeState(ItemStack stack, AmpouleState state) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(ORIGIN_KEY, state.originDimension());
            tag.putLong(EXPIRY_KEY, state.expiresAtMillis());
        });
    }

    public ItemStack emptyStack() {
        return new ItemStack(CFItems.FLUID_AMPOULE.get());
    }

    public static int capacity() {
        try {
            return CaveFactoryConfig.AMPOULE_CAPACITY.get();
        } catch (IllegalStateException ignored) {
            return DEFAULT_CAPACITY;
        }
    }

    public ItemStack residueFor(AmpouleState state) {
        String residue = state.originDimension();
        for (MachineType machine : MachineType.values()) {
            if (machine.dimension().dimensionId().equals(residue)) {
                return new ItemStack(CFItems.residue(machine).get());
            }
        }
        return ItemStack.EMPTY;
    }
    @Override
    public boolean hasStoredFluid(ItemStack stack) {
        return !storedFluid(stack).isEmpty();
    }

    @Override
    public AmpouleExpiry.Settlement observe(
        ItemStack stack,
        String observedDimension,
        long nowMillis,
        long windowMillis
    ) {
        if (!hasStoredFluid(stack)) {
            return new AmpouleExpiry.Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState state = readState(stack);
        if (!state.hasTimer() && state.originDimension().isEmpty()) {
            writeState(stack, AmpouleState.fresh(observedDimension));
            return new AmpouleExpiry.Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState observed = AmpouleState.observeDimension(state, observedDimension, nowMillis, windowMillis);
        if (!observed.equals(state)) {
            writeState(stack, observed);
        }
        return new AmpouleExpiry.Settlement(stack, ItemStack.EMPTY, false);
    }

    @Override
    public AmpouleExpiry.Settlement settle(ItemStack stack, long nowMillis) {
        if (!hasStoredFluid(stack)) {
            return new AmpouleExpiry.Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState state = readState(stack);
        if (!state.isExpired(nowMillis)) {
            return new AmpouleExpiry.Settlement(stack, ItemStack.EMPTY, false);
        }
        return new AmpouleExpiry.Settlement(emptyStack(), residueFor(state), true);
    }

    @Override
    public boolean mergeable(ItemStack first, ItemStack second) {
        return second.getItem() instanceof FluidAmpouleItem other
            && readState(first).canMerge(other.readState(second));
    }

    @Override
    public ItemStack prepareForChannelStorage(ItemStack stack, long nowMillis) {
        AmpouleState state = readState(stack);
        if (state.isExpired(nowMillis)) {
            return ItemStack.EMPTY;
        }
        ItemStack prepared = stack.copy();
        AmpouleState withExpiry = new AmpouleState(
            state.originDimension(),
            nowMillis + (long) CaveFactoryConfig.STABILITY_SECONDS.get() * 1_000L
        );
        writeState(prepared, withExpiry);
        return prepared;
    }

    public static void setOrigin(ItemStack stack, FactoryDimension dimension) {
        if (!(stack.getItem() instanceof FluidAmpouleItem ampoule)) {
            return;
        }
        AmpouleState state = ampoule.readState(stack);
        if (state.originDimension().isEmpty()) {
            ampoule.writeState(stack, AmpouleState.fresh(dimension.dimensionId()));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        Player player = context.getPlayer();
        if (!hasStoredFluid(held) || player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack single = held.copyWithCount(1);
        long now = System.currentTimeMillis();
        AmpouleExpiry.observe(
            single,
            context.getLevel().dimension().location().toString(),
            now,
            CaveFactoryConfig.STABILITY_SECONDS.get() * 1_000L
        );
        AmpouleExpiry.Settlement settlement = AmpouleExpiry.settle(single, now);
        if (settlement.expired()) {
            held.shrink(1);
            giveBack(player, settlement.stack());
            if (!settlement.residue().isEmpty()) {
                giveBack(player, settlement.residue());
            }
            return InteractionResult.SUCCESS;
        }

        IFluidHandler target = FluidUtil.getFluidHandler(
            context.getLevel(),
            context.getClickedPos(),
            context.getClickedFace()
        ).orElse(null);
        IFluidHandlerItem source = FluidUtil.getFluidHandler(single).orElse(null);
        if (target == null || source == null) {
            return InteractionResult.PASS;
        }
        FluidStack stored = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        int accepted = target.fill(stored, IFluidHandler.FluidAction.SIMULATE);
        if (stored.isEmpty() || accepted <= 0) {
            return InteractionResult.PASS;
        }
        FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return InteractionResult.PASS;
        }
        target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        held.shrink(1);
        giveBack(player, single);
        return InteractionResult.SUCCESS;
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || stack.isEmpty()) {
            return;
        }
        AmpouleExpiry.observe(
            stack,
            level.dimension().location().toString(),
            System.currentTimeMillis(),
            CaveFactoryConfig.STABILITY_SECONDS.get() * 1_000L
        );
        AmpouleExpiry.Settlement settled = AmpouleExpiry.settle(stack, System.currentTimeMillis());
        if (!settled.expired()) {
            return;
        }

        int expiredCount = stack.getCount();
        AmpouleFluidHandler.clearStoredState(stack);
        if (!settled.residue().isEmpty()) {
            ItemStack residue = settled.residue().copyWithCount(expiredCount);
            if (!player.getInventory().add(residue)) {
                player.drop(residue, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null) {
            tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.ampoule.empty")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        FluidStack fluid = storedFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.ampoule.empty")
                .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable(
                "tooltip.dimensionworks_cave_factory.ampoule.fluid",
                fluid.getDisplayName(),
                fluid.getAmount()
            ).withStyle(ChatFormatting.AQUA));
        }
        String origin = tag.getString(ORIGIN_KEY);
        if (!origin.isEmpty()) {
            tooltip.add(Component.translatable(
                "tooltip.dimensionworks_cave_factory.ampoule.origin",
                origin
            ).withStyle(ChatFormatting.DARK_GRAY));
        }
        long expiry = tag.getLong(EXPIRY_KEY);
        if (expiry > 0) {
            tooltip.add(Component.translatable(
                "tooltip.dimensionworks_cave_factory.ampoule.expiry",
                Math.max(0, expiry - System.currentTimeMillis()) / 1000
            ).withStyle(ChatFormatting.RED));
        }
    }

    public static final class AmpouleFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;

        public AmpouleFluidHandler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? storedFluid(container).copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && container.getCount() == 1 && CFFluids.isFactoryFluid(stack)
                && (getFluidInTank(0).isEmpty() || FluidStack.isSameFluidSameComponents(getFluidInTank(0), stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) {
                return 0;
            }
            FluidStack current = getFluidInTank(0);
            int room = capacity() - current.getAmount();
            int accepted = Math.min(room, resource.getAmount());
            if (accepted <= 0 || action.simulate()) {
                return accepted;
            }
            FluidStack result = current.isEmpty()
                ? resource.copyWithAmount(accepted)
                : current.copyWithAmount(current.getAmount() + accepted);
            writeStoredFluid(container, result);
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(Math.min(current.getAmount(), resource.getAmount()), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(current.getAmount(), maxDrain);
            FluidStack result = current.copyWithAmount(drained);
            if (action.execute()) {
                if (drained >= current.getAmount()) {
                    clearStoredState(container);
                } else {
                    writeStoredFluid(container, current.copyWithAmount(current.getAmount() - drained));
                }
            }
            return result;
        }

    private static void writeStoredFluid(ItemStack stack, FluidStack fluid) {
        stack.set(CFDataComponents.AMPOULE_FLUID.get(), AmpouleFluid.of(fluid));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(FLUID_KEY));
    }

        private static void clearStoredState(ItemStack stack) {
            stack.remove(CFDataComponents.AMPOULE_FLUID.get());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(FLUID_KEY);
                tag.remove(ORIGIN_KEY);
                tag.remove(EXPIRY_KEY);
                tag.remove(RESIDUE_KEY);
            });
        }
    }
}
