package dev.szx.dimensionworks.pressurecrystals;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import dev.szx.dimensionworks.pressurecrystals.mixin.VacuumChamberModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

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

            Fluid source = crystallized.getFluid();
            if (source instanceof FlowingFluid flowing) source = flowing.getSource();
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(source);
            if (id == null || !id.getNamespace().equals("dimensionworks_cave_factory")
                || !id.getPath().startsWith(CRYSTALLIZED_PREFIX)) continue;

            String metal = id.getPath().substring(CRYSTALLIZED_PREFIX.length());
            ResourceLocation moltenId = ResourceLocation.fromNamespaceAndPath("dimensionworks", "molten_" + metal);
            Fluid molten = BuiltInRegistries.FLUID.getOptional(moltenId)
                .orElseThrow(() -> new IllegalStateException("Missing ordinary molten fluid for " + id));

            ItemStack crystalStack = new ItemStack(crystal, CRYSTAL_OUTPUT);
            if (tier != PressureTier.NONE) {
                CompoundTag traits = new CompoundTag();
                traits.putString("Metal", metal);
                CompoundTag custom = new CompoundTag();
                custom.put("DimensionWorks", traits);
                crystalStack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            }
            List<ItemStack> output = List.of(crystalStack);
            if (!basin.acceptOutputs(output, List.of(), true)) continue;
            if (!basin.acceptOutputs(output, List.of(), false)) return;

            int remaining = crystallized.getAmount() - PRECIPITATION_AMOUNT;
            tank.setFluid(remaining > 0 ? new FluidStack(molten.builtInRegistryHolder(), remaining, crystallized.getComponentsPatch()) : FluidStack.EMPTY);
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
