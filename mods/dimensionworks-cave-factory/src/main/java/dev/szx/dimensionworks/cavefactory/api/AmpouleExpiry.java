package dev.szx.dimensionworks.cavefactory.api;

import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import dev.szx.dimensionworks.cavefactory.logic.AmpouleState;
import net.minecraft.world.item.ItemStack;

public final class AmpouleExpiry {
    public record Settlement(ItemStack stack, ItemStack residue, boolean expired) {
        public AmpouleSettlementResult asResult() {
            return new AmpouleSettlementResult(stack, residue, expired);
        }
    }

    public record AmpouleSettlementResult(ItemStack stack, ItemStack residue, boolean expired) {}

    private AmpouleExpiry() {}

    public static Settlement observe(ItemStack stack, String observedDimension, long nowMillis, long windowMillis) {
        if (!(stack.getItem() instanceof FluidAmpouleItem ampoule)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        if (!ampoule.hasStoredFluid(stack)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState state = ampoule.readState(stack);
        if (!state.hasTimer() && state.originDimension().isEmpty()) {
            state = AmpouleState.fresh(observedDimension);
            ampoule.writeState(stack, state);
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState observed = AmpouleState.observeDimension(state, observedDimension, nowMillis, windowMillis);
        if (!observed.equals(state)) {
            ampoule.writeState(stack, observed);
        }
        return new Settlement(stack, ItemStack.EMPTY, false);
    }

    public static Settlement settle(ItemStack stack, long nowMillis) {
        if (!(stack.getItem() instanceof FluidAmpouleItem ampoule)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        if (!ampoule.hasStoredFluid(stack)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        AmpouleState state = ampoule.readState(stack);
        if (!state.isExpired(nowMillis)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        ItemStack residue = ampoule.residueFor(state);
        ItemStack empty = ampoule.emptyStack();
        return new Settlement(empty, residue, true);
    }

    public static boolean mergeable(ItemStack first, ItemStack second) {
        if (!(first.getItem() instanceof FluidAmpouleItem firstAmpoule)
            || !(second.getItem() instanceof FluidAmpouleItem secondAmpoule)) {
            return false;
        }
        return firstAmpoule.readState(first).canMerge(secondAmpoule.readState(second));
    }
}
