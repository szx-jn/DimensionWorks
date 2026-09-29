package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.core.StressTransfer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MEGearboxBlockEntity.class, remap = false)
public abstract class MEGearboxBlockEntityMixin {
    @Unique
    private boolean dimensionworks$exportRunning;
    @Unique
    private int dimensionworks$outputRpm;
    @Unique
    private float dimensionworks$advertisedStressPerRpm;

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$getActualOutputSpeed(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (dimensionworks$exportRunning && self.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
            cir.setReturnValue(Math.copySign(dimensionworks$outputRpm, self.getConfiguredSpeed()));
        }
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$advertiseOnlyChargedStress(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (dimensionworks$exportRunning && self.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
            cir.setReturnValue(dimensionworks$advertisedStressPerRpm);
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

        int configuredRpm = Math.abs(self.getConfiguredSpeed());
        if (configuredRpm <= 0) {
            dimensionworks$setExportState(false, 0, 0.0F);
            return;
        }

        IMemoryGridService service = self.getMainNode().getGrid() == null
            ? null
            : self.getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service == null) {
            dimensionworks$deactivate();
            return;
        }

        long tick = self.getLevel().getGameTime();
        GlobalPos gearboxPos = GlobalPos.of(self.getLevel().dimension(), self.getBlockPos());
        double fullSpeedLoad = dimensionworks$fullSpeedLoad(self, configuredRpm);
        long requestedSu = fullSpeedLoad <= 0.0D ? 0L : (long) Math.ceil(fullSpeedLoad);
        service.reportGearboxExport(gearboxPos, configuredRpm, requestedSu, tick);

        double q = service.finalQ(tick);
        int outputRpm = StressTransfer.outputRpm(configuredRpm, q);
        long proportionalRequest = outputRpm <= 0 || fullSpeedLoad <= 0.0D
            ? 0L
            : Math.max(1L, (long) Math.ceil(fullSpeedLoad * outputRpm / configuredRpm));
        long available = proportionalRequest <= 0L
            ? 0L
            : service.extractSu(proportionalRequest, Actionable.SIMULATE, tick);
        long charge = StressTransfer.chargedSu(proportionalRequest, available);
        long charged = charge <= 0L ? 0L : service.extractSu(charge, Actionable.MODULATE, tick);
        float advertisedStress = StressTransfer.capacityPerRpm(charged, outputRpm);

        dimensionworks$setExportState(true, outputRpm, advertisedStress);
    }

    @Unique
    private void dimensionworks$deactivate() {
        dimensionworks$setExportState(false, 0, 0.0F);
    }

    @Unique
    private void dimensionworks$setExportState(boolean running, int outputRpm, float advertisedStress) {
        boolean changed = dimensionworks$exportRunning != running
            || dimensionworks$outputRpm != outputRpm
            || Float.compare(dimensionworks$advertisedStressPerRpm, advertisedStress) != 0;
        dimensionworks$exportRunning = running;
        dimensionworks$outputRpm = outputRpm;
        dimensionworks$advertisedStressPerRpm = advertisedStress;
        if (changed) {
            ((MEGearboxBlockEntity) (Object) this).updateGeneratedRotation();
        }
    }

    @Unique
    private static double dimensionworks$fullSpeedLoad(MEGearboxBlockEntity gearbox, int configuredRpm) {
        if (!gearbox.hasNetwork()) {
            return 0.0D;
        }
        KineticNetwork network = gearbox.getOrCreateNetwork();
        List<Float> stressPerRpm = new ArrayList<>(network.members.size());
        for (var entry : network.members.entrySet()) {
            KineticBlockEntity member = entry.getKey();
            if (member != gearbox) {
                stressPerRpm.add(entry.getValue());
            }
        }
        return StressTransfer.fullSpeedLoad(stressPerRpm, configuredRpm);
    }
}
