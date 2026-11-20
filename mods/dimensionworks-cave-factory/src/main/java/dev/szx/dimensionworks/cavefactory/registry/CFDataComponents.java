package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.FluidStack;
import dev.szx.dimensionworks.cavefactory.item.AmpouleFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class CFDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister.create(
        Registries.DATA_COMPONENT_TYPE, DimensionWorksCaveFactory.MOD_ID);
    public static final Supplier<DataComponentType<AmpouleFluid>> AMPOULE_FLUID = REGISTER.register("ampoule_fluid", () ->
        DataComponentType.<AmpouleFluid>builder().persistent(FluidStack.CODEC.xmap(AmpouleFluid::of, AmpouleFluid::toStack))
            .networkSynchronized(FluidStack.STREAM_CODEC.map(AmpouleFluid::of, AmpouleFluid::toStack)).build());
    private CFDataComponents() {}
}
