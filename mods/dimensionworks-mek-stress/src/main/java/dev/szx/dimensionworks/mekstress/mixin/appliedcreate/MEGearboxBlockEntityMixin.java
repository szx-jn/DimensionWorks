package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.mekstress.api.IMEGearboxExportState;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.core.StressTransfer;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MEGearboxBlockEntity.class, remap = false)
public abstract class MEGearboxBlockEntityMixin implements IMEGearboxExportState {
    @Shadow private boolean hasStressSupply;
    @Unique private boolean dimensionworks$exportRunning;
    @Unique private boolean dimensionworks$overloaded;
    @Unique private int dimensionworks$outputRpm;
    @Unique private long dimensionworks$reservedSu;

    @Override public boolean dimensionworks$isExportRunning() { return dimensionworks$exportRunning; }
    @Override public int dimensionworks$getExportOutputRpm() { return dimensionworks$outputRpm; }
    @Override public long dimensionworks$getExportReservedSu() { return dimensionworks$reservedSu; }
    @Override public boolean dimensionworks$isStressOverloaded() { return dimensionworks$overloaded; }

    @Inject(method = {"tickImport", "tickExport"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$disableStoredEnergyTransfer(IEnergySource energy, MEStorage storage,
                                                           IActionSource source, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void dimensionworks$refreshLiveAllocation(CallbackInfo ci) { dimensionworks$refresh(); }

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$getActualOutputSpeed(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getMode() != MEGearboxBlockEntity.Mode.EXPORT) return;
        dimensionworks$refresh();
        cir.setReturnValue(dimensionworks$exportRunning
            ? Math.copySign(dimensionworks$outputRpm, self.getConfiguredSpeed()) : 0.0F);
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$advertiseReservedCapacity(CallbackInfoReturnable<Float> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getMode() == MEGearboxBlockEntity.Mode.EXPORT)
            cir.setReturnValue(dimensionworks$exportRunning
                ? StressTransfer.capacityPerRpm(dimensionworks$reservedSu, dimensionworks$outputRpm) : 0.0F);
    }

    @Unique private void dimensionworks$refresh() {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) return;
        if (self.isRemoved() || !self.getMainNode().isActive() || self.getMainNode().getGrid() == null) {
            dimensionworks$applyExportState(false, 0, 0, true);
            return;
        }
        IMemoryGridService service = self.getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service == null) dimensionworks$applyExportState(false, 0, 0, true);
        else service.snapshot(self.getLevel().getGameTime());
    }

    @Override public void dimensionworks$applyExportState(boolean running, int rpm, long reservedSu,
                                                        boolean overloaded) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        running &= self.getMode() == MEGearboxBlockEntity.Mode.EXPORT;
        int outputRpm = running ? rpm : 0;
        long capacity = running ? reservedSu : 0;
        boolean changed = dimensionworks$exportRunning != running || dimensionworks$outputRpm != outputRpm
            || dimensionworks$reservedSu != capacity || dimensionworks$overloaded != overloaded;
        dimensionworks$exportRunning = running;
        dimensionworks$outputRpm = outputRpm;
        dimensionworks$reservedSu = capacity;
        dimensionworks$overloaded = overloaded;
        hasStressSupply = running;
        if (changed && self.getLevel() != null && !self.getLevel().isClientSide && !self.isRemoved()) {
            self.updateGeneratedRotation();
            KineticNetworkOptimizer.invalidateAndRefreshFor(self);
            self.sendData();
        }
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void dimensionworks$writeLiveState(CompoundTag tag, boolean packet, CallbackInfo ci) {
        // Display state only. Server reads never restore an allocation from NBT.
        tag.putBoolean("DWExportRunning", dimensionworks$exportRunning);
        tag.putBoolean("DWStressOverloaded", dimensionworks$overloaded);
        tag.putInt("DWOutputRpm", dimensionworks$outputRpm);
        tag.putLong("DWReservedSu", dimensionworks$reservedSu);
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void dimensionworks$readDisplayState(CompoundTag tag, boolean packet, CallbackInfo ci) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        if (!packet) return;
        dimensionworks$exportRunning = tag.getBoolean("DWExportRunning");
        dimensionworks$overloaded = tag.getBoolean("DWStressOverloaded");
        dimensionworks$outputRpm = tag.getInt("DWOutputRpm");
        dimensionworks$reservedSu = tag.getLong("DWReservedSu");
    }

    @Inject(method = "addToGoggleTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$overloadTooltip(List<Component> tooltip, boolean sneaking,
                                               CallbackInfoReturnable<Boolean> cir) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        tooltip.add(Component.translatable("goggles.dimensionworks_mek_stress."
            + (self.getMode() == MEGearboxBlockEntity.Mode.EXPORT ? "output" : "input")));
        tooltip.add(Component.translatable("goggles.dimensionworks_mek_stress.coefficient",
            Float.toString(self.getConfiguredStress())).withStyle(ChatFormatting.GRAY));
        if (self.getMode() == MEGearboxBlockEntity.Mode.EXPORT)
            tooltip.add(Component.translatable("goggles.dimensionworks_mek_stress.live_output",
                dimensionworks$outputRpm, dimensionworks$reservedSu).withStyle(ChatFormatting.AQUA));
        if (self.getMode() == MEGearboxBlockEntity.Mode.EXPORT && dimensionworks$overloaded) {
            tooltip.add(Component.translatable("goggles.dimensionworks_mek_stress.stopped")
                .withStyle(ChatFormatting.RED));
        }
        cir.setReturnValue(true);
    }
}
