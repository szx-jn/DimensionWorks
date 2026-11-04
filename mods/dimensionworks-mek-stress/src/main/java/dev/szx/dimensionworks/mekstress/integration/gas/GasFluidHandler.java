package dev.szx.dimensionworks.mekstress.integration.gas;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Combines an endpoint's existing fluid and gas capabilities for Create only. */
public final class GasFluidHandler implements IFluidHandler {
    private static final Map<BlockEntity, List<CacheEntry>> CACHE = new WeakHashMap<>();
    private record CacheEntry(WeakReference<LazyOptional<IFluidHandler>> fluid,
                              WeakReference<LazyOptional<IGasHandler>> gas,
                              WeakReference<LazyOptional<IFluidHandler>> combined) {}
    private final WeakReference<BlockEntity> owner;
    private final Direction side;

    private GasFluidHandler(BlockEntity owner, Direction side) { this.owner = new WeakReference<>(owner); this.side = side; }
    public static synchronized LazyOptional<IFluidHandler> capability(BlockEntity owner, Capability<IFluidHandler> requested, Direction side) {
        LazyOptional<IFluidHandler> fluid = owner.getCapability(requested, side);
        LazyOptional<IGasHandler> gas = owner.getCapability(Capabilities.GAS_HANDLER, side);
        if (!gas.isPresent()) return fluid;
        List<CacheEntry> entries = CACHE.computeIfAbsent(owner, ignored -> new ArrayList<>());
        entries.removeIf(entry -> {
            LazyOptional<IFluidHandler> optional = entry.combined().get();
            return optional == null || !optional.isPresent();
        });
        for (CacheEntry entry : entries) {
            if (entry.fluid().get() == fluid && entry.gas().get() == gas) {
                LazyOptional<IFluidHandler> cached = entry.combined().get();
                if (cached != null && cached.isPresent()) return cached;
            }
        }
        GasFluidHandler handler = new GasFluidHandler(owner, side);
        LazyOptional<IFluidHandler> combined = LazyOptional.of(() -> handler);
        gas.addListener(ignored -> combined.invalidate());
        if (fluid.isPresent()) fluid.addListener(ignored -> combined.invalidate());
        entries.add(new CacheEntry(new WeakReference<>(fluid), new WeakReference<>(gas), new WeakReference<>(combined)));
        return combined;
    }
    private BlockEntity owner() { BlockEntity be = owner.get(); return be == null || be.isRemoved() ? null : be; }
    private IFluidHandler fluid() {
        BlockEntity be = owner();
        return be == null ? null : be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }
    private IGasHandler gas() {
        BlockEntity be = owner();
        return be == null ? null : be.getCapability(Capabilities.GAS_HANDLER, side).orElse(null);
    }
    private static Action action(FluidAction action) { return action.execute() ? Action.EXECUTE : Action.SIMULATE; }
    @Override public int getTanks() {
        IFluidHandler f = fluid(); IGasHandler g = gas();
        return (f == null ? 0 : f.getTanks()) + (g == null ? 0 : g.getTanks());
    }
    @Override public FluidStack getFluidInTank(int tank) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return f.getFluidInTank(tank);
        IGasHandler g = gas(); return g == null || tank - count >= g.getTanks() ? FluidStack.EMPTY : GasFluid.encode(g.getChemicalInTank(tank - count));
    }
    @Override public int getTankCapacity(int tank) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return f.getTankCapacity(tank);
        IGasHandler g = gas(); return g == null || tank - count >= g.getTanks() ? 0 : (int) Math.min(Integer.MAX_VALUE, g.getTankCapacity(tank - count));
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return !GasFluid.isGas(stack) && f.isFluidValid(tank, stack);
        IGasHandler g = gas(); GasStack gasStack = GasFluid.decode(stack);
        return g != null && tank - count < g.getTanks() && !gasStack.isEmpty() && g.isValid(tank - count, gasStack);
    }
    @Override public int fill(FluidStack stack, FluidAction action) {
        if (!GasFluid.isGas(stack)) { IFluidHandler f = fluid(); return f == null ? 0 : f.fill(stack, action); }
        IGasHandler g = gas(); GasStack gasStack = GasFluid.decode(stack);
        if (g == null || gasStack.isEmpty()) return 0;
        return (int) (gasStack.getAmount() - g.insertChemical(gasStack, action(action)).getAmount());
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        if (!GasFluid.isGas(stack)) { IFluidHandler f = fluid(); return f == null ? FluidStack.EMPTY : f.drain(stack, action); }
        IGasHandler g = gas(); GasStack gasStack = GasFluid.decode(stack);
        return g == null || gasStack.isEmpty() ? FluidStack.EMPTY : GasFluid.encode(g.extractChemical(gasStack, action(action)));
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        IFluidHandler f = fluid();
        FluidStack result = f == null ? FluidStack.EMPTY : f.drain(amount, action);
        if (!result.isEmpty()) return result;
        IGasHandler g = gas(); return g == null ? FluidStack.EMPTY : GasFluid.encode(g.extractChemical(amount, action(action)));
    }
}
