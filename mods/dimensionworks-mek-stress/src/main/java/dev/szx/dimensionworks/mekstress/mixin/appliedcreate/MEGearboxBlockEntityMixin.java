package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import net.minecraft.core.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MEGearboxBlockEntity.class, remap = false)
public abstract class MEGearboxBlockEntityMixin {
    @Unique
    private int dimensionworks$originalSpeed;
    @Unique
    private long dimensionworks$scaledTransferRate;
    @Unique
    private boolean dimensionworks$scaled;

    @Shadow
    private long getTransferRate(float speed) {
        throw new AssertionError();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void dimensionworks$prepareSharedScheduling(CallbackInfo ci) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        dimensionworks$scaled = false;
        if (self.getLevel() == null || self.getLevel().isClientSide()
            || self.getMode() != MEGearboxBlockEntity.Mode.EXPORT) {
            return;
        }
        IMemoryGridService service = self.getMainNode().getGrid() == null
            ? null
            : self.getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service == null) {
            return;
        }

        int speed = Math.abs(self.getConfiguredSpeed());
        long requestedRate = getTransferRate(speed);
        service.reportGearboxExport(GlobalPos.of(self.getLevel().dimension(), self.getBlockPos()), speed, requestedRate,
            self.getLevel().getGameTime());
        double q = service.finalQ(self.getLevel().getGameTime());

        dimensionworks$originalSpeed = self.getConfiguredSpeed();
        dimensionworks$scaledTransferRate = Math.round(requestedRate * q);
        self.setConfiguredSpeed((int) Math.round(dimensionworks$originalSpeed * q));
        dimensionworks$scaled = true;
    }

    @Redirect(
        method = "tickExport",
        at = @At(
            value = "INVOKE",
            target = "Lcom/loliball/appliedcreate/energy/MEGearboxBlockEntity;getTransferRate(F)J"
        ),
        remap = false
    )
    private long dimensionworks$scaleExportRate(MEGearboxBlockEntity self, float speed) {
        return dimensionworks$scaled ? dimensionworks$scaledTransferRate : getTransferRate(speed);
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void dimensionworks$restoreConfiguration(CallbackInfo ci) {
        if (!dimensionworks$scaled) {
            return;
        }
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        self.setConfiguredSpeed(dimensionworks$originalSpeed);
        dimensionworks$scaled = false;
    }
}
