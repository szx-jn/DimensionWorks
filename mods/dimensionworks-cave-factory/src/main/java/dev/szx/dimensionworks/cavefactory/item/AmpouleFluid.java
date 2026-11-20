package dev.szx.dimensionworks.cavefactory.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable component value: a mutable FluidStack cannot safely be stored in item components. */
public record AmpouleFluid(Holder<Fluid> fluid, int amount, DataComponentPatch components) {
    public AmpouleFluid {
        java.util.Objects.requireNonNull(fluid);
        java.util.Objects.requireNonNull(components);
        if (amount <= 0) throw new IllegalArgumentException("Stored ampoule fluid must be positive");
    }
    public static AmpouleFluid of(FluidStack stack) { return new AmpouleFluid(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch()); }
    public FluidStack toStack() { return new FluidStack(fluid, amount, components); }
}
