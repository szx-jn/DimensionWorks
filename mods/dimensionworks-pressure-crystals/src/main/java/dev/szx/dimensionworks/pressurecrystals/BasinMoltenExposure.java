package dev.szx.dimensionworks.pressurecrystals;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Continuous exposure of the basin's two input fluids, saved with the basin. */
public final class BasinMoltenExposure {
    private static final long EXPOSURE_TICKS = 600;
    private static final ResourceLocation LID = ResourceLocation.fromNamespaceAndPath("createdieselgenerators", "basin_lid");
    private final FluidStack[] observed = {FluidStack.EMPTY, FluidStack.EMPTY};
    private final long[] started = {-1, -1};

    public void observe(BasinBlockEntity basin) {
        boolean open = isOpen(basin);
        long now = basin.getLevel().getGameTime();
        for (int i = 0; i < observed.length; i++) {
            FluidStack current = tank(basin, i).getFluid();
            if (!FluidStack.matches(observed[i], current)) {
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
            var crystalId = ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "crystallized_" + id.getPath());
            var crystal = BuiltInRegistries.FLUID.getOptional(crystalId)
                .orElseThrow(() -> new IllegalStateException("Missing crystallized fluid for " + id));
            tank(basin, i).setFluid(new FluidStack(crystal.builtInRegistryHolder(), current.getAmount(), current.getComponentsPatch()));
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
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return id != null && id.getNamespace().equals("dimensionworks")
            && id.getPath().startsWith("molten_") ? id : null;
    }

    private static boolean isOpen(BasinBlockEntity basin) {
        var cover = basin.getLevel().getBlockState(basin.getBlockPos().above());
        return cover.isAir() || LID.equals(BuiltInRegistries.BLOCK.getKey(cover.getBlock()))
            && cover.hasProperty(BlockStateProperties.OPEN) && cover.getValue(BlockStateProperties.OPEN);
    }

    private static SmartFluidTank tank(BasinBlockEntity basin, int slot) {
        return ((BasinTankAccessor) (Object) basin.inputTank.getTanks()[slot]).dimensionworks$getTank();
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (int i = 0; i < observed.length; i++) {
            CompoundTag entry = new CompoundTag();
            entry.put("Fluid", observed[i].saveOptional(registries));
            entry.putLong("Started", started[i]);
            entries.add(entry);
        }
        tag.put("MoltenExposure", entries);
    }

    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("MoltenExposure")) {
            for (int i = 0; i < observed.length; i++) {
                observed[i] = FluidStack.EMPTY;
                started[i] = -1;
            }
            return;
        }
        if (!tag.contains("MoltenExposure", Tag.TAG_LIST))
            throw new IllegalArgumentException("MoltenExposure must be a list");
        ListTag entries = tag.getList("MoltenExposure", Tag.TAG_COMPOUND);
        if (entries.size() != observed.length)
            throw new IllegalArgumentException("MoltenExposure must contain exactly two input tanks");
        FluidStack[] decoded = new FluidStack[observed.length];
        long[] times = new long[observed.length];
        for (int i = 0; i < observed.length; i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.contains("Fluid", Tag.TAG_COMPOUND) || !entry.contains("Started", Tag.TAG_LONG))
                throw new IllegalArgumentException("MoltenExposure requires a fluid and long timestamp");
            decoded[i] = FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
                entry.getCompound("Fluid")).getOrThrow(IllegalArgumentException::new);
            times[i] = entry.getLong("Started");
            if (times[i] < -1 || times[i] >= 0 && moltenId(decoded[i]) == null)
                throw new IllegalArgumentException("MoltenExposure timestamp requires ordinary molten metal");
        }
        System.arraycopy(decoded, 0, observed, 0, observed.length);
        System.arraycopy(times, 0, started, 0, started.length);
    }
}
