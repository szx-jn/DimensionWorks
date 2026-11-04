package dev.szx.dimensionworks.mekstress.integration.gas;

import com.simibubi.create.content.fluids.VirtualFluid;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import java.util.function.Consumer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.material.Fluid;

/** Internal transport representation. Gas identity is part of the fluid stack tag. */
public final class GasFluid {
    private static final String GAS_KEY = "DimensionWorksGas";
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, DimensionWorksMekStress.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, DimensionWorksMekStress.MOD_ID);
    public static final RegistryObject<FluidType> TYPE = TYPES.register("transport_gas", () -> new GasType(FluidType.Properties.create().density(-100).viscosity(100)));
    public static final RegistryObject<Flowing> SOURCE = FLUIDS.register("transport_gas", () -> new Flowing(properties(), true));
    public static final RegistryObject<Flowing> FLOWING = FLUIDS.register("flowing_transport_gas", () -> new Flowing(properties(), false));

    private GasFluid() {}
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    private static ForgeFlowingFluid.Properties properties() { return new ForgeFlowingFluid.Properties(TYPE, SOURCE, FLOWING); }
    public static FluidStack encode(GasStack gas) {
        if (gas.isEmpty()) return FluidStack.EMPTY;
        ResourceLocation id = MekanismAPI.gasRegistry().getKey(gas.getType());
        if (id == null) return FluidStack.EMPTY;
        FluidStack result = new FluidStack(SOURCE.get(), (int) Math.min(Integer.MAX_VALUE, gas.getAmount()));
        result.getOrCreateTag().putString(GAS_KEY, id.toString());
        return result;
    }
    public static boolean isGas(FluidStack stack) { return !stack.isEmpty() && stack.getFluid() == SOURCE.get(); }
    public static GasStack decode(FluidStack stack) {
        if (!isGas(stack) || stack.getTag() == null) return GasStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString(GAS_KEY));
        Gas gas = id == null ? null : MekanismAPI.gasRegistry().getValue(id);
        return gas == null || gas.isEmptyType() ? GasStack.EMPTY : new GasStack(gas, stack.getAmount());
    }
    public static final class GasType extends FluidType {
        public GasType(Properties properties) { super(properties); }
        @Override public String getDescriptionId(FluidStack stack) {
            GasStack gas = decode(stack);
            return gas.isEmpty() ? super.getDescriptionId(stack) : gas.getType().getTranslationKey();
        }
        @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return new ResourceLocation("minecraft", "block/water_still"); }
                @Override public ResourceLocation getFlowingTexture() { return new ResourceLocation("minecraft", "block/water_flow"); }
                @Override public int getTintColor(FluidStack stack) {
                    GasStack gas = decode(stack);
                    return gas.isEmpty() ? 0xffffffff : 0xff000000 | gas.getType().getTint();
                }
            });
        }
    }
    public static final class Flowing extends VirtualFluid {
        public Flowing(Properties properties, boolean source) { super(properties, source); }
    }
}
