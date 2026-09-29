package dev.szx.dimensionworks.mekstress.mixin.mek;

import appeng.api.config.Actionable;
import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.nbt.CompoundTag;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class BasicEnergyContainerMixin implements StressContainerOwner {

    @Shadow
    private FloatingLong stored;

    @Shadow
    @Final
    private FloatingLong maxEnergy;

    @Shadow
    @Final
    protected Predicate<AutomationType> canExtract;

    @Unique
    @Nullable
    private TileEntityMekanism dimensionworks$owner;

    @Override
    public void dimensionworks$setOwner(@Nullable TileEntityMekanism tile) {
        dimensionworks$owner = tile;
    }

    @Override
    @Nullable
    public TileEntityMekanism dimensionworks$getOwner() {
        return dimensionworks$owner;
    }

    @Override
    public double dimensionworks$storedJoules() {
        return stored.doubleValue();
    }

    @Override
    public double dimensionworks$capacityJoules() {
        return maxEnergy.doubleValue();
    }

    @Override
    public double dimensionworks$insertJoulesDirect(double joules, Actionable mode) {
        TileEntityMekanism owner = dimensionworks$owner;
        if (owner == null || StressRules.isDisabledEnergyInfrastructure(owner) || !(joules > 0.0D)) {
            return 0.0D;
        }
        double room = Math.max(0.0D, maxEnergy.doubleValue() - stored.doubleValue());
        double accepted = Math.min(joules, room);
        if (accepted <= 0.0D || mode == Actionable.SIMULATE) {
            return accepted;
        }

        FloatingLong previous = stored;
        stored = stored.add(FloatingLong.create(accepted)).min(maxEnergy);
        double inserted = stored.subtract(previous).doubleValue();
        if (inserted > 0.0D) {
            dimensionworks$onContentsChanged();
        }
        return inserted;
    }

    @Inject(method = "getEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$getStressEnergy(CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism owner = dimensionworks$owner;
        if (owner == null) {
            return;
        }
        cir.setReturnValue(StressRules.isDisabledEnergyInfrastructure(owner) ? FloatingLong.ZERO : stored);
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$blockFeInsert(FloatingLong amount, Action action, AutomationType automationType,
                                               CallbackInfoReturnable<FloatingLong> cir) {
        if (dimensionworks$owner != null) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$consumeStress(FloatingLong amount, Action action, AutomationType automationType,
                                              CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism owner = dimensionworks$owner;
        if (owner == null) {
            return;
        }
        if (StressRules.isDisabledEnergyInfrastructure(owner) || !canExtract.test(automationType)) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }

        long tick = owner.getLevel() == null ? 0L : owner.getLevel().getGameTime();
        if (!StressRules.powerState(owner).hasQualifyingSource(tick)) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }

        FloatingLong extracted = amount.min(stored).copy();
        if (action.execute() && !extracted.isZero()) {
            stored = stored.subtract(extracted);
            dimensionworks$onContentsChanged();
        }
        cir.setReturnValue(extracted);
    }

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$stressBufferEmpty(CallbackInfoReturnable<Boolean> cir) {
        TileEntityMekanism owner = dimensionworks$owner;
        if (owner == null) {
            return;
        }
        cir.setReturnValue(StressRules.isDisabledEnergyInfrastructure(owner) || stored.isZero());
    }

    private void dimensionworks$onContentsChanged() {
        ((BasicEnergyContainer) (Object) this).onContentsChanged();
    }

    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void dimensionworks$preserveLegacyFe(CallbackInfoReturnable<CompoundTag> cir) {
        if (dimensionworks$owner != null && stored != null && !stored.isZero()) {
            cir.getReturnValue().putString("stored", stored.toString());
        }
    }
}
