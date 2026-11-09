package dev.szx.dimensionworks.pressurecrystals.mixin;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
    targets = "com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour$TankSegment",
    remap = false
)
public interface BasinTankAccessor {
    @Accessor(value = "tank", remap = false)
    SmartFluidTank dimensionworks$getTank();
}
