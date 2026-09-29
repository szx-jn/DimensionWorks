package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.api.StressSupplySource;
import dev.szx.dimensionworks.mekstress.core.StressEnergyBuffer;
import dev.szx.dimensionworks.mekstress.core.StressPowerState;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import dev.szx.dimensionworks.mekstress.core.StressMachineTier;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMixin implements StressPoweredMachine {

    @Override
    @Nullable
    public StressEnergyBuffer dimensionworks$stressEnergyBuffer() {
        TileEntityMekanism self = (TileEntityMekanism) (Object) this;
        for (IEnergyContainer container : self.getEnergyContainers(null)) {
            if (container instanceof StressEnergyBuffer buffer) {
                return buffer;
            }
        }
        return null;
    }

    @Override
    public void dimensionworks$rechargeFromStress(long gameTick) {
        dimensionworks$stressPowerState().tryFillBuffer(gameTick);
    }

    @Override
    public int dimensionworks$requiredRpm() {
        TileEntityMekanism self = (TileEntityMekanism) (Object) this;
        BaseTier baseTier = Attribute.getBaseTier(self.getBlockType());
        return StressMachineTier.forBaseTier(baseTier == null ? null : baseTier.name()).requiredRpm();
    }

    @Unique
    private StressPowerState dimensionworks$stressPowerState;

    @Unique
    private boolean dimensionworks$repeatingUpdates;

    @Override
    public StressPowerState dimensionworks$stressPowerState() {
        if (dimensionworks$stressPowerState == null) {
            dimensionworks$stressPowerState = new StressPowerState(this);
        }
        return dimensionworks$stressPowerState;
    }

    @Override
    public void dimensionworks$registerStressSource(StressSupplySource source, long gameTick) {
        dimensionworks$stressPowerState().register(source, gameTick);
    }

    @Override
    public double dimensionworks$speedMultiplier(long gameTick) {
        return dimensionworks$stressPowerState().speedMultiplier(gameTick);
    }

    @Override
    public int dimensionworks$batches(long gameTick) {
        return dimensionworks$stressPowerState().batches(gameTick);
    }

    @Override
    public boolean dimensionworks$disabledEnergyInfrastructure() {
        return StressRules.isDisabledEnergyInfrastructure((TileEntityMekanism) (Object) this);
    }

    @Inject(method = "onUpdateServer", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$runOverspeedBatches(CallbackInfo ci) {
        if (dimensionworks$repeatingUpdates) {
            return;
        }

        TileEntityMekanism self = (TileEntityMekanism) (Object) this;
        if (StressRules.isDisabledEnergyInfrastructure(self)) {
            ci.cancel();
            return;
        }

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        int batches = dimensionworks$batches(level.getGameTime());
        if (batches <= 1) {
            return;
        }

        dimensionworks$repeatingUpdates = true;
        try {
            TileEntityMekanismInvoker invoker = (TileEntityMekanismInvoker) this;
            for (int i = 1; i < batches; i++) {
                invoker.dimensionworks$onUpdateServer();
            }
        } finally {
            dimensionworks$repeatingUpdates = false;
        }
    }
}
