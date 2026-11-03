package dev.szx.dimensionworks.createculling.mixin;

import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public interface VisualAccess {
    @Accessor("blockEntity") BlockEntity dimensionworks$getBlockEntity();
}
