package dev.szx.dimensionworks.mekstress.blockentity;

import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Shared channel fluid view in mB. 1 B is represented as 1000 mB. */
final class CrossDimensionFluidHandler implements IFluidHandler {
    private final CrossDimensionTransportBlockEntity endpoint;
    CrossDimensionFluidHandler(CrossDimensionTransportBlockEntity endpoint) { this.endpoint = endpoint; }

    private ChannelStockSavedData data() {
        return endpoint.getLevel() instanceof ServerLevel server && endpoint.channel() > 0
            ? ChannelStockSavedData.get(server) : null;
    }

    private List<FluidStack> visible() {
        ChannelStockSavedData data = data();
        return data == null || !endpoint.canExportFluids() ? List.of()
            : data.visibleFluids(endpoint.channel(), endpoint.dimensionId());
    }

    @Override public int getTanks() { return visible().size(); }
    @Override public FluidStack getFluidInTank(int tank) {
        ChannelStockSavedData data = data();
        return data == null || !endpoint.canExportFluids() ? FluidStack.EMPTY
            : data.fluidAt(endpoint.channel(), endpoint.dimensionId(), tank);
    }
    @Override public int getTankCapacity(int tank) {
        ChannelStockSavedData data = data();
        return data == null ? 0 : data.fluidCapacity(endpoint.channel());
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return !stack.isEmpty() && !ChannelStockSavedData.requiresAmpoule(stack) && endpoint.canImportFluids();
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !endpoint.canImportFluids()) return 0;
        ChannelStockSavedData data = data();
        return data == null ? 0 : data.fill(endpoint.channel(), endpoint.dimensionId(), resource,
            action.simulate());
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !endpoint.canExportFluids()) return FluidStack.EMPTY;
        ChannelStockSavedData data = data();
        return data == null ? FluidStack.EMPTY : data.drain(endpoint.channel(), endpoint.dimensionId(), resource,
            action.simulate());
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || !endpoint.canExportFluids()) return FluidStack.EMPTY;
        ChannelStockSavedData data = data();
        return data == null ? FluidStack.EMPTY : data.drain(endpoint.channel(), endpoint.dimensionId(), 0,
            maxDrain, action.simulate());
    }
}
