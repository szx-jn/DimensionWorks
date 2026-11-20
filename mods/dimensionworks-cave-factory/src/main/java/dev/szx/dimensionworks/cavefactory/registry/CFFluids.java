package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.Map;

public final class CFFluids {
    public record FluidEntry(
        MachineType machine,
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> still,
        DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock> block
    ) {}

    private static final Map<MachineType, FluidEntry> BY_MACHINE = new EnumMap<>(MachineType.class);
    private static boolean registered;

    private CFFluids() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (MachineType machine : MachineType.values()) {
            registerFluid(machine, colorFor(machine));
        }
    }

    public static FluidEntry get(MachineType machine) {
        return BY_MACHINE.get(machine);
    }

    public static boolean isFactoryFluid(net.neoforged.neoforge.fluids.FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (FluidEntry entry : BY_MACHINE.values()) {
            if (stack.getFluid() == entry.still().get() || stack.getFluid() == entry.flowing().get()) {
                return true;
            }
        }
        return false;
    }

    private static void registerFluid(MachineType machine, int color) {
        String id = machine.fluidId();
        DeferredHolder<FluidType, FluidType> type = DimensionWorksCaveFactory.CFRegistry.FLUID_TYPES.register(
            id,
            () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type.dimensionworks_cave_factory." + id)
                .density(1_100)
                .viscosity(1_400))
        );

        DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> still =
            DimensionWorksCaveFactory.CFRegistry.FLUIDS.register(
                id,
                () -> new BaseFlowingFluid.Source(properties(machine))
            );

        DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> flowing =
            DimensionWorksCaveFactory.CFRegistry.FLUIDS.register(
                id + "_flowing",
                () -> new BaseFlowingFluid.Flowing(properties(machine))
            );

        DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock> block =
            DimensionWorksCaveFactory.CFRegistry.BLOCKS.register(
                id,
                () -> new LiquidBlock(still.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable())
            );

        BY_MACHINE.put(machine, new FluidEntry(machine, type, still, flowing, block));
    }

    private static BaseFlowingFluid.Properties properties(MachineType machine) {
        return new BaseFlowingFluid.Properties(
            () -> BY_MACHINE.get(machine).type().get(),
            () -> BY_MACHINE.get(machine).still().get(),
            () -> BY_MACHINE.get(machine).flowing().get()
        ).block(() -> BY_MACHINE.get(machine).block().get());
    }

    public static int colorFor(MachineType machine) {
        return switch (machine) {
            case MAGNETIC -> 0xFFE85D5D;
            case PRIMORDIAL -> 0xFF6FAE4B;
            case TOXIC -> 0xFF9EDB43;
            case ABYSSAL -> 0xFF2B8CB8;
            case FORLORN -> 0xFF6D4A8C;
            case CANDY -> 0xFFF07AB4;
        };
    }
}
