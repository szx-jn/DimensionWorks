package dev.szx.dimensionworks.mekstress.integration.gas;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Combines an endpoint's existing fluid and gas capabilities for Create only. */
public final class GasFluidHandler implements IFluidHandler {
    private static final Map<BlockEntity, Map<Direction, WeakReference<GasFluidHandler>>> CACHE = new WeakHashMap<>();
    private final WeakReference<BlockEntity> owner;
    private final Direction side;
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IFluidHandler, Direction> fluidCache;
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IChemicalHandler, Direction> chemicalCache;

    private GasFluidHandler(BlockEntity owner, Direction side) {
        this.owner = new WeakReference<>(owner); this.side = side;
        if (owner.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            fluidCache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, level, owner.getBlockPos(), side);
            chemicalCache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                Capabilities.CHEMICAL.block(), level, owner.getBlockPos(), side);
        } else { fluidCache = null; chemicalCache = null; }
    }
    public static synchronized IFluidHandler capability(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        var be = level.getBlockEntity(pos);
        if (be == null) return null;
        var entries = CACHE.computeIfAbsent(be, ignored -> new java.util.HashMap<>());
        var reference = entries.get(side);
        var combined = reference == null ? null : reference.get();
        if (combined == null) {
            combined = new GasFluidHandler(be, side);
            entries.put(side, new WeakReference<>(combined));
        }
        return combined.gas() == null ? combined.fluid() : combined;
    }
    private BlockEntity owner() { BlockEntity be = owner.get(); return be == null || be.isRemoved() ? null : be; }
    private IFluidHandler fluid() {
        BlockEntity be = owner();
        if (be == null) return null;
        return fluidCache != null ? fluidCache.getCapability() : be.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, be.getBlockPos(), side);
    }
    private IChemicalHandler gas() {
        BlockEntity be = owner();
        if (be == null) return null;
        return chemicalCache != null ? chemicalCache.getCapability() : be.getLevel().getCapability(
            Capabilities.CHEMICAL.block(), be.getBlockPos(), side);
    }
    private static Action action(FluidAction action) { return action.execute() ? Action.EXECUTE : Action.SIMULATE; }
    @Override public int getTanks() {
        IFluidHandler f = fluid(); IChemicalHandler g = gas();
        return (f == null ? 0 : f.getTanks()) + (g == null ? 0 : g.getChemicalTanks());
    }
    @Override public FluidStack getFluidInTank(int tank) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return f.getFluidInTank(tank);
        IChemicalHandler g = gas(); return g == null || tank - count >= g.getChemicalTanks() ? FluidStack.EMPTY : GasFluid.encode(g.getChemicalInTank(tank - count));
    }
    @Override public int getTankCapacity(int tank) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return f.getTankCapacity(tank);
        IChemicalHandler g = gas(); return g == null || tank - count >= g.getChemicalTanks() ? 0 : (int) Math.min(Integer.MAX_VALUE, g.getChemicalTankCapacity(tank - count));
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        IFluidHandler f = fluid(); int count = f == null ? 0 : f.getTanks();
        if (tank < count) return !GasFluid.isGas(stack) && f.isFluidValid(tank, stack);
        IChemicalHandler g = gas(); ChemicalStack gasStack = GasFluid.decode(stack);
        return g != null && tank - count < g.getChemicalTanks() && !gasStack.isEmpty() && g.isValid(tank - count, gasStack);
    }
    @Override public int fill(FluidStack stack, FluidAction action) {
        if (!GasFluid.isGas(stack)) { IFluidHandler f = fluid(); return f == null ? 0 : f.fill(stack, action); }
        IChemicalHandler g = gas(); ChemicalStack gasStack = GasFluid.decode(stack);
        if (g == null || gasStack.isEmpty()) return 0;
        return (int) (gasStack.getAmount() - g.insertChemical(gasStack, action(action)).getAmount());
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        if (!GasFluid.isGas(stack)) { IFluidHandler f = fluid(); return f == null ? FluidStack.EMPTY : f.drain(stack, action); }
        IChemicalHandler g = gas(); ChemicalStack gasStack = GasFluid.decode(stack);
        return g == null || gasStack.isEmpty() ? FluidStack.EMPTY : GasFluid.encode(g.extractChemical(gasStack, action(action)));
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        IFluidHandler f = fluid();
        FluidStack result = f == null ? FluidStack.EMPTY : f.drain(amount, action);
        if (!result.isEmpty()) return result;
        IChemicalHandler g = gas(); return g == null ? FluidStack.EMPTY : GasFluid.encode(g.extractChemical(amount, action(action)));
    }
}
