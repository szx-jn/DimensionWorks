package dev.szx.dimensionworks.cavefactory.item;

import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.logic.AmpouleState;
import dev.szx.dimensionworks.cavefactory.logic.FactoryDimension;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidAmpouleItem extends Item {
    public static final int DEFAULT_CAPACITY = 250;
    public static final String FLUID_KEY = "Fluid";
    public static final String ORIGIN_KEY = "OriginDimension";
    public static final String EXPIRY_KEY = "ExpiresAtEpochMs";
    public static final String RESIDUE_KEY = "SourceResidue";

    public FluidAmpouleItem(Properties properties) {
        super(properties);
    }

    public AmpouleState readState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return AmpouleState.fresh("");
        }
        String origin = tag.getString(ORIGIN_KEY);
        long expiry = tag.getLong(EXPIRY_KEY);
        return new AmpouleState(origin, expiry);
    }

    public void writeState(ItemStack stack, AmpouleState state) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ORIGIN_KEY, state.originDimension());
        tag.putLong(EXPIRY_KEY, state.expiresAtMillis());
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
    public boolean hasStoredFluid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(FLUID_KEY);
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
        stack.setTag(null);
        if (!settled.residue().isEmpty()) {
            ItemStack residue = settled.residue().copyWithCount(expiredCount);
            if (!player.getInventory().add(residue)) {
                player.drop(residue, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.ampoule.empty")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_KEY));
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

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new AmpouleFluidHandler(stack);
    }

    private static final class AmpouleFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
        private final ItemStack container;

        private AmpouleFluidHandler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container.copy();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            CompoundTag tag = container.getTag();
            if (tag == null || !tag.contains(FLUID_KEY)) {
                return FluidStack.EMPTY;
            }
            return FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_KEY));
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return CFFluids.isFactoryFluid(stack);
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
                ? new FluidStack(resource, accepted)
                : new FluidStack(current, current.getAmount() + accepted);
            container.getOrCreateTag().put(FLUID_KEY, result.writeToNBT(new CompoundTag()));
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || !current.isFluidEqual(resource)) {
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
            FluidStack result = new FluidStack(current, drained);
            if (action.execute()) {
                CompoundTag tag = container.getOrCreateTag();
                if (drained >= current.getAmount()) {
                    clearStoredState(container);
                } else {
                    tag.put(FLUID_KEY, new FluidStack(current, current.getAmount() - drained)
                        .writeToNBT(new CompoundTag()));
                }
            }
            return result;
        }

        private static void clearStoredState(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return;
            }
            tag.remove(FLUID_KEY);
            tag.remove(ORIGIN_KEY);
            tag.remove(EXPIRY_KEY);
            tag.remove(RESIDUE_KEY);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
            return capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM
                ? net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, LazyOptional.of(() -> this))
                : LazyOptional.empty();
        }
    }
}
