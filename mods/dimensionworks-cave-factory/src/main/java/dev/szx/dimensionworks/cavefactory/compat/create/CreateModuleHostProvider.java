package dev.szx.dimensionworks.cavefactory.compat.create;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Host state lives on the owning block entity; inventories persist in its native persistent data. */
public final class CreateModuleHostProvider {
    private CreateModuleHostProvider() {}

    public static MachineCapability capabilityFor(BlockEntity owner) { return capabilityFor(owner.getClass()); }

    public static MachineCapability capabilityFor(Class<?> type) {
        if (MechanicalMixerBlockEntity.class.isAssignableFrom(type)
            || MechanicalPressBlockEntity.class.isAssignableFrom(type)) {
            return MachineCapability.ITEM_AND_MAPPED_RECIPES;
        }
        if (SawBlockEntity.class.isAssignableFrom(type) || MillstoneBlockEntity.class.isAssignableFrom(type)
            || CrushingWheelControllerBlockEntity.class.isAssignableFrom(type)) {
            return MachineCapability.ITEM_ONLY;
        }
        return MachineCapability.NONE;
    }
}
