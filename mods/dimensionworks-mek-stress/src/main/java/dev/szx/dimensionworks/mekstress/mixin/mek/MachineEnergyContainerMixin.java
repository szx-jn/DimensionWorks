package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import dev.szx.dimensionworks.mekstress.integration.mek.MechanicalEnergy;
import dev.szx.dimensionworks.mekstress.core.PowerBudget;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class MachineEnergyContainerMixin implements dev.szx.dimensionworks.mekstress.api.IMechanicalEnergyBudget {
    @Unique private final PowerBudget dimensionworks$budget = new PowerBudget();
    @Unique private long dimensionworks$rateTick = Long.MIN_VALUE;
    @Unique private double dimensionworks$reservedRate;

    @Override public void dimensionworks$prepareBudget(long tick, double joulesPerTick) {
        dimensionworks$rateTick = tick;
        dimensionworks$reservedRate = joulesPerTick;
    }

    @Unique private FloatingLong dimensionworks$available(TileEntityMekanism tile) {
        double rate = dimensionworks$rateTick == tile.getLevel().getGameTime() ? dimensionworks$reservedRate : 0;
        return FloatingLong.create(dimensionworks$budget.available(tile.getLevel().getGameTime(), rate));
    }
    @Inject(method = "getEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$getMechanicalEnergy(CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$serverMachineTile();
        if (tile == null || !StressRules.isEligible(tile)) {
            return;
        }
        cir.setReturnValue(MachinePowerManager.hasMechanicalPower(tile)
            ? dimensionworks$available(tile)
            : FloatingLong.ZERO);
    }

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$isMechanicallyEmpty(CallbackInfoReturnable<Boolean> cir) {
        TileEntityMekanism tile = dimensionworks$serverMachineTile();
        if (tile != null && StressRules.isEligible(tile)) {
            cir.setReturnValue(!MachinePowerManager.hasMechanicalPower(tile) || dimensionworks$available(tile).isZero());
        }
    }

    @Inject(method = "serializeNBT", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$doNotPersistMechanicalEnergy(CallbackInfoReturnable<CompoundTag> cir) {
        TileEntityMekanism tile = dimensionworks$serverMachineTile();
        if (tile != null && StressRules.isEligible(tile)) {
            // Mechanical power is derived from the live Create route and is never stored as FE.
            cir.setReturnValue(new CompoundTag());
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$rejectFe(FloatingLong amount, Action action, AutomationType automationType,
                                         CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$serverMachineTile();
        if (tile != null && StressRules.isEligible(tile)) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$extractMechanicalEnergy(FloatingLong amount, Action action, AutomationType automationType,
                                                       CallbackInfoReturnable<FloatingLong> cir) {
        TileEntityMekanism tile = dimensionworks$serverMachineTile();
        if (tile == null || !StressRules.isEligible(tile) || amount.isZero()) {
            return;
        }
        if (automationType != AutomationType.INTERNAL || !MachinePowerManager.consumeForProcessing(tile)) {
            cir.setReturnValue(FloatingLong.ZERO);
            return;
        }
        double accepted = dimensionworks$budget.extract(tile.getLevel().getGameTime(),
            dimensionworks$rateTick == tile.getLevel().getGameTime() ? dimensionworks$reservedRate : 0,
            amount.doubleValue(), action == Action.EXECUTE);
        cir.setReturnValue(FloatingLong.create(accepted));
    }

    private TileEntityMekanism dimensionworks$machineTile() {
        return MechanicalEnergy.owner(this);
    }

    private TileEntityMekanism dimensionworks$serverMachineTile() {
        TileEntityMekanism tile = dimensionworks$machineTile();
        return tile == null || tile.getLevel() == null || tile.getLevel().isClientSide
            ? null
            : tile;
    }
}
