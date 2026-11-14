package dev.szx.dimensionworks.pressurecrystals;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Continuous exposure of the basin's two input fluids, saved with the basin. */
public final class BasinMoltenExposure {
    private static final long EXPOSURE_TICKS = 600;
    private static final ResourceLocation LID = new ResourceLocation("createdieselgenerators", "basin_lid");
    private final FluidStack[] observed = {FluidStack.EMPTY, FluidStack.EMPTY};
    private final long[] started = {-1, -1};

    public void observe(BasinBlockEntity basin) {
        boolean open = isOpen(basin);
        long now = basin.getLevel().getGameTime();
        for (int i = 0; i < observed.length; i++) {
            FluidStack current = tank(basin, i).getFluid();
            if (!observed[i].isFluidStackIdentical(current)) {
                observed[i] = current.copy();
                started[i] = open && moltenId(current) != null ? now : -1;
                basin.setChanged();
            } else if (!open && started[i] >= 0) {
                started[i] = -1;
                basin.setChanged();
            } else if (open && started[i] < 0 && moltenId(current) != null) {
                started[i] = now;
                basin.setChanged();
            }
        }
    }

    public void tick(BasinBlockEntity basin) {
        observe(basin);
        long now = basin.getLevel().getGameTime();
        for (int i = 0; i < observed.length; i++) {
            if (started[i] < 0 || now - started[i] < EXPOSURE_TICKS) continue;
            FluidStack current = tank(basin, i).getFluid();
            ResourceLocation id = moltenId(current);
            var crystal = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(
                "dimensionworks_cave_factory", "crystallized_" + id.getPath()));
            if (crystal == null) throw new IllegalStateException("Missing crystallized fluid for " + id);
            tank(basin, i).setFluid(new FluidStack(crystal, current.getAmount(), current.getTag()));
            observed[i] = FluidStack.EMPTY;
            started[i] = -1;
            basin.notifyChangeOfContents();
            basin.notifyUpdate();
        }
    }

    private static ResourceLocation moltenId(FluidStack stack) {
        if (stack.isEmpty()) return null;
        var fluid = stack.getFluid();
        if (fluid instanceof FlowingFluid flowing) fluid = flowing.getSource();
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id != null && id.getNamespace().equals("dimensionworks")
            && id.getPath().startsWith("molten_") ? id : null;
    }

    private static boolean isOpen(BasinBlockEntity basin) {
        var cover = basin.getLevel().getBlockState(basin.getBlockPos().above());
        return cover.isAir() || LID.equals(ForgeRegistries.BLOCKS.getKey(cover.getBlock()))
            && cover.hasProperty(BlockStateProperties.OPEN) && cover.getValue(BlockStateProperties.OPEN);
    }

    private static SmartFluidTank tank(BasinBlockEntity basin, int slot) {
        return ((BasinTankAccessor) (Object) basin.inputTank.getTanks()[slot]).dimensionworks$getTank();
    }

    public void write(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (int i = 0; i < observed.length; i++) {
            CompoundTag entry = new CompoundTag();
            entry.put("Fluid", observed[i].writeToNBT(new CompoundTag()));
            entry.putLong("Started", started[i]);
            entries.add(entry);
        }
        tag.put("MoltenExposure", entries);
    }

    public void read(CompoundTag tag) {
        ListTag entries = tag.getList("MoltenExposure", Tag.TAG_COMPOUND);
        for (int i = 0; i < observed.length; i++) {
            observed[i] = i < entries.size() ? FluidStack.loadFluidStackFromNBT(entries.getCompound(i).getCompound("Fluid"))
                : FluidStack.EMPTY;
            started[i] = i < entries.size() ? entries.getCompound(i).getLong("Started") : -1;
        }
    }
}
