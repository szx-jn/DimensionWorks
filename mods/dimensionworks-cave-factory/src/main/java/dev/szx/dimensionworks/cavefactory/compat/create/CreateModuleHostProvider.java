package dev.szx.dimensionworks.cavefactory.compat.create;

import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public final class CreateModuleHostProvider implements ICapabilityProvider {
    private final LazyOptional<ModuleHost> host;

    public CreateModuleHostProvider(BlockEntity owner, MachineCapability capability) {
        this.host = LazyOptional.of(() -> new CreateModuleHost(owner, capability));
    }

    public static MachineCapability capabilityFor(BlockEntity blockEntity) {
        return capabilityFor(blockEntity.getClass());
    }

    public static MachineCapability capabilityFor(Class<?> type) {
        String name = type.getName();
        if (name.endsWith("MechanicalMixerBlockEntity")
            || name.endsWith("MechanicalPressBlockEntity")) {
            return MachineCapability.ITEM_AND_MAPPED_RECIPES;
        }
        if (name.endsWith("SawBlockEntity")
            || name.endsWith("MillstoneBlockEntity")
            || name.endsWith("CrushingWheelControllerBlockEntity")) {
            return MachineCapability.ITEM_ONLY;
        }
        return MachineCapability.NONE;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == ModuleHost.CAPABILITY ? host.cast() : LazyOptional.empty();
    }
}
