package dev.szx.dimensionworks.cavefactory.blockentity;

import dev.szx.dimensionworks.cavefactory.logic.PortMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Manages the six directional port modes, capability exposure, and NBT serialization for factory machines. */
final class MachinePortController {
    private final Map<Direction, PortMode> portModes = new EnumMap<>(Direction.class);

    MachinePortController() {
        for (Direction direction : Direction.values()) {
            portModes.put(direction, PortMode.DISABLED);
        }
    }

    PortMode mode(Direction direction) {
        return portModes.getOrDefault(direction, PortMode.DISABLED);
    }

    void cycle(Direction direction, @Nullable Level level, BlockPos machinePos) {
        PortMode next = mode(direction).next();
        if (next == PortMode.KINETIC_INPUT && hasKineticPort(direction)) {
            next = next.next();
        }
        portModes.put(direction, next);
        if (level != null) {
            level.invalidateCapabilities(machinePos);
            for (Direction adjacent : Direction.values()) {
                level.invalidateCapabilities(machinePos.relative(adjacent));
            }
            BlockEntity port = level.getBlockEntity(machinePos.relative(direction));
            if (port != null) {
                level.invalidateCapabilities(port.getBlockPos());
                level.sendBlockUpdated(port.getBlockPos(), port.getBlockState(), port.getBlockState(), 3);
            }
        }
    }

    boolean hasKineticPort(Direction except) {
        for (Map.Entry<Direction, PortMode> entry : portModes.entrySet()) {
            if (entry.getKey() != except && entry.getValue() == PortMode.KINETIC_INPUT) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    Direction kineticPort() {
        for (Map.Entry<Direction, PortMode> entry : portModes.entrySet()) {
            if (entry.getValue() == PortMode.KINETIC_INPUT) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    IItemHandler itemHandler(@Nullable Direction side, boolean removed,
                              ItemStackHandler inputItems, IItemHandler outputItemCapability) {
        if (side == null || removed) {
            return null;
        }
        return switch (mode(side)) {
            case ITEM_INPUT -> inputItems;
            case ITEM_OUTPUT -> outputItemCapability;
            default -> null;
        };
    }

    @Nullable
    IFluidHandler fluidHandler(@Nullable Direction side, boolean removed,
                                FluidTank inputTank, FluidTank outputTank) {
        if (side == null || removed) {
            return null;
        }
        return switch (mode(side)) {
            case FLUID_INPUT -> inputTank;
            case FLUID_OUTPUT -> outputTank;
            default -> null;
        };
    }

    CompoundTag writeNbt() {
        CompoundTag modes = new CompoundTag();
        for (Direction direction : Direction.values()) {
            modes.putString(direction.getName(), mode(direction).id());
        }
        return modes;
    }

    void readNbt(CompoundTag modes) {
        for (Direction direction : Direction.values()) {
            String id = modes.getString(direction.getName());
            PortMode mode = PortMode.DISABLED;
            for (PortMode candidate : PortMode.values()) {
                if (candidate.id().equals(id)) {
                    mode = candidate;
                    break;
                }
            }
            portModes.put(direction, mode);
        }
    }
}
