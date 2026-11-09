package dev.szx.dimensionworks.pressurecrystals.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.szx.dimensionworks.pressurecrystals.PressureCrystallization;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinCrystallizationMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$precipitateCrystals(CallbackInfo ci) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level = basin.getLevel();
        if (level == null || level.isClientSide || level.getGameTime() % 200L != 0L) return;
        PressureCrystallization.process(basin);
    }
}
