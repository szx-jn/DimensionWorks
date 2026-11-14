package dev.szx.dimensionworks.pressurecrystals.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.szx.dimensionworks.pressurecrystals.BasinMoltenExposure;
import dev.szx.dimensionworks.pressurecrystals.PressureCrystallization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinCrystallizationMixin {
    @Unique
    private final BasinMoltenExposure dimensionworks$moltenExposure = new BasinMoltenExposure();

    // BasinBlockEntity returns early when its contents are unchanged. Cover every
    // return so idle basins still settle crystals on the 200-tick interval.
    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void dimensionworks$precipitateCrystals(CallbackInfo ci) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level = basin.getLevel();
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 20L == 0L) dimensionworks$moltenExposure.tick(basin);
        if (level.getGameTime() % 200L == 0L) PressureCrystallization.process(basin);
    }

    @Inject(method = "notifyChangeOfContents", at = @At("HEAD"), remap = false)
    private void dimensionworks$observeMoltenInputs(CallbackInfo ci) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level = basin.getLevel();
        if (level != null && !level.isClientSide) dimensionworks$moltenExposure.observe(basin);
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void dimensionworks$readExposure(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket) dimensionworks$moltenExposure.read(tag);
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void dimensionworks$writeExposure(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket) dimensionworks$moltenExposure.write(tag);
    }
}
