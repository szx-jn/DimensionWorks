package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.mekstress.api.IMEGearboxExportState;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.core.StressTransfer;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import net.minecraft.core.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MEGearboxBlockEntity.class, remap = false)
public abstract class MEGearboxBlockEntityMixin implements IMEGearboxExportState {
    @Unique
    private boolean dimensionworks$exportRunning;
    @Unique
    private int dimensionworks$outputRpm;
    @Unique
    private float dimensionworks$advertisedStressPerRpm;
    @Unique
    private long dimensionworks$chargedSu;

    @Override
    @Unique
    public boolean dimensionworks$isExportRunning() {
        return dimensionworks$exportRunning;
    }

    @Override
    @Unique
    public int dimensionworks$getExportOutputRpm() {
        return dimensionworks$outputRpm;
    }

    @Override
    @Unique
    public long dimensionworks$getExportChargedSu() {
        return dimensionworks$chargedSu;
    }

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$getActualOutputSpeed(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getMode() != MEGearboxBlockEntity.Mode.EXPORT) {
            return;
        }
        cir.setReturnValue(dimensionworks$exportRunning
            ? Math.copySign(dimensionworks$outputRpm, self.getConfiguredSpeed())
            : 0.0F);
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$advertiseOnlyChargedStress(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
            cir.setReturnValue(dimensionworks$exportRunning ? dimensionworks$advertisedStressPerRpm : 0.0F);
        }
    }

    @Inject(method = "tickExport", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$tickExactExport(IEnergySource energy, MEStorage storage, IActionSource actionSource,
                                                CallbackInfo ci) {
        ci.cancel();
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide()
            || self.getMode() != MEGearboxBlockEntity.Mode.EXPORT) {
            dimensionworks$deactivate();
            return;
        }

        IMemoryGridService service = self.getMainNode().getGrid() == null
            ? null
            : self.getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service == null) {
            dimensionworks$deactivate();
            return;
        }

        MachinePowerManager.GearboxDemand demand = MachinePowerManager.inspectGearbox(self);
        int requestedRpm = demand == null ? 0 : demand.requestedRpm();
        long requestedSu = demand == null ? 0L : demand.requestedSuPerTick();
        long tick = self.getLevel().getGameTime();
        GlobalPos gearboxPos = GlobalPos.of(self.getLevel().dimension(), self.getBlockPos());
        service.reportGearboxExport(gearboxPos, requestedRpm, requestedSu, tick);

        if (requestedRpm <= 0 || requestedSu <= 0L) {
            dimensionworks$deactivate();
            return;
        }

        double q = service.finalQ(tick);
        if (q <= 0.0D) {
            dimensionworks$deactivate();
            return;
        }

        int outputRpm = StressTransfer.outputRpm(requestedRpm, q);
        long proportionalRequest = outputRpm <= 0
            ? 0L
            : saturatedCeil(demand.fullSpeedLoad() * outputRpm / requestedRpm);
        long available = proportionalRequest <= 0L
            ? 0L
            : service.extractSu(proportionalRequest, Actionable.SIMULATE, tick);
        long charge = StressTransfer.chargedSu(proportionalRequest, available);
        long charged = charge <= 0L ? 0L : service.extractSu(charge, Actionable.MODULATE, tick);
        if (!StressTransfer.canRun(requestedRpm, demand.fullSpeedLoad(), outputRpm, charged)) {
            dimensionworks$deactivate();
            return;
        }
        dimensionworks$setExportState(true, outputRpm,
            StressTransfer.capacityPerRpm(charged, outputRpm), charged);
    }

    @Unique
    private void dimensionworks$deactivate() {
        dimensionworks$setExportState(false, 0, 0.0F, 0L);
    }

    @Unique
    private void dimensionworks$setExportState(boolean running, int outputRpm, float advertisedStress, long chargedSu) {
        boolean changed = dimensionworks$exportRunning != running
            || dimensionworks$outputRpm != outputRpm
            || Float.compare(dimensionworks$advertisedStressPerRpm, advertisedStress) != 0
            || dimensionworks$chargedSu != chargedSu;
        dimensionworks$exportRunning = running;
        dimensionworks$outputRpm = outputRpm;
        dimensionworks$advertisedStressPerRpm = advertisedStress;
        dimensionworks$chargedSu = chargedSu;
        if (changed) {
            ((MEGearboxBlockEntity) (Object) this).updateGeneratedRotation();
        }
    }

    @Unique
    private static long saturatedCeil(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0L;
        }
        return value >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(value);
    }
}
