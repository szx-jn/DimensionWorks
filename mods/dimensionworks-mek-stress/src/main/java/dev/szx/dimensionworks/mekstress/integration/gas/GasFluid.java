package dev.szx.dimensionworks.mekstress.integration.gas;

import com.simibubi.create.content.fluids.VirtualFluid;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.material.Fluid;

/** Internal transport representation. Chemical identity is part of the fluid stack tag. */
public final class GasFluid {
    private static final String GAS_KEY = "DimensionWorksGas";
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, DimensionWorksMekStress.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, DimensionWorksMekStress.MOD_ID);
    public static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("transport_gas", () -> new GasType(FluidType.Properties.create().density(-100).viscosity(100)));
    public static final DeferredHolder<Fluid, Flowing> SOURCE = FLUIDS.register("transport_gas", () -> new Flowing(properties(), true));
    public static final DeferredHolder<Fluid, Flowing> FLOWING = FLUIDS.register("flowing_transport_gas", () -> new Flowing(properties(), false));

    private GasFluid() {}
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    private static BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING); }
    public static FluidStack encode(ChemicalStack gas) {
        if (gas.isEmpty()) return FluidStack.EMPTY;
        ResourceLocation id = MekanismAPI.CHEMICAL_REGISTRY.getKey(gas.getChemical());
        if (id == null) return FluidStack.EMPTY;
        FluidStack result = new FluidStack(SOURCE.get(), (int) Math.min(Integer.MAX_VALUE, gas.getAmount()));
        var identity = new net.minecraft.nbt.CompoundTag(); identity.putString(GAS_KEY, id.toString());
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(identity));
        return result;
    }
    public static boolean isGas(FluidStack stack) { return !stack.isEmpty() && stack.getFluid() == SOURCE.get(); }
    public static ChemicalStack decode(FluidStack stack) {
        if (!isGas(stack) || !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return ChemicalStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getString(GAS_KEY));
        Chemical gas = id == null ? null : MekanismAPI.CHEMICAL_REGISTRY.getOptional(id).orElse(null);
        return gas == null || gas.isEmptyType() ? ChemicalStack.EMPTY : new ChemicalStack(gas, stack.getAmount());
    }
    public static final class GasType extends FluidType {
        public GasType(Properties properties) { super(properties); }
        @Override public String getDescriptionId(FluidStack stack) {
            ChemicalStack gas = decode(stack);
            return gas.isEmpty() ? super.getDescriptionId(stack) : gas.getChemical().getTranslationKey();
        }
    }
    public static final class Flowing extends VirtualFluid {
        public Flowing(Properties properties, boolean source) { super(properties, source); }
    }
}
