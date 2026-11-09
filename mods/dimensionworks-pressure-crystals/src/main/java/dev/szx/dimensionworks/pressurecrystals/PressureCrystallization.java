package dev.szx.dimensionworks.pressurecrystals;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import dev.szx.dimensionworks.pressurecrystals.mixin.VacuumChamberModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class PressureCrystallization {
    private static final int PRECIPITATION_AMOUNT = 600;
    private static final int CRYSTAL_OUTPUT = 3;
    private static final String CRYSTALLIZED_PREFIX = "crystallized_molten_";

    private PressureCrystallization() {}

    public static void process(BasinBlockEntity basin) {
        PressureTier tier = getPressureTier(basin);
        Item crystal = switch (tier) {
            case NONE -> DimensionWorksPressureCrystals.USELESS_CRYSTAL.get();
            case LOW -> DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get();
            case MEDIUM -> DimensionWorksPressureCrystals.MEDIUM_PRESSURE_CRYSTAL.get();
            case HIGH -> DimensionWorksPressureCrystals.HIGH_PRESSURE_CRYSTAL.get();
        };

        for (var segment : basin.inputTank.getTanks()) {
            SmartFluidTank tank = ((BasinTankAccessor) (Object) segment).dimensionworks$getTank();
            FluidStack crystallized = tank.getFluid().copy();
            if (crystallized.getAmount() < PRECIPITATION_AMOUNT) continue;

            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(crystallized.getFluid());
            if (id == null || !id.getNamespace().equals("dimensionworks_cave_factory")
                || !id.getPath().startsWith(CRYSTALLIZED_PREFIX)) continue;

            String metal = id.getPath().substring(CRYSTALLIZED_PREFIX.length());
            Fluid molten = ForgeRegistries.FLUIDS.getValue(
                new ResourceLocation("dimensionworks", "molten_" + metal));
            if (molten == null) continue;

            ItemStack crystalStack = new ItemStack(crystal, CRYSTAL_OUTPUT);
            if (tier != PressureTier.NONE) {
                crystalStack.getOrCreateTagElement("DimensionWorks").putString("Metal", metal);
            }
            List<ItemStack> output = List.of(crystalStack);
            if (!basin.acceptOutputs(output, List.of(), true)) return;
            if (!basin.acceptOutputs(output, List.of(), false)) return;

            int remaining = crystallized.getAmount() - PRECIPITATION_AMOUNT;
            tank.setFluid(remaining > 0 ? new FluidStack(molten, remaining) : FluidStack.EMPTY);
            basin.notifyChangeOfContents();
            basin.notifyUpdate();
            return;
        }
    }

    private static PressureTier getPressureTier(BasinBlockEntity basin) {
        BlockPos pos = basin.getBlockPos().above(2);
        BlockEntity compressor = basin.getLevel().getBlockEntity(pos);
        if (!(compressor instanceof KineticBlockEntity kinetic)
            || !(compressor instanceof VacuumChamberModeAccessor mode)
            || !mode.dimensionworks$isPressurizing()) return PressureTier.NONE;
        return PressureTier.fromRpm(Math.abs(kinetic.getSpeed()));
    }
}
