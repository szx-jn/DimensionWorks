package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin {

    @Shadow
    protected boolean overStressed;

    /**
     * Caps the value every consumer sees without changing Create's raw kinetic network.
     *
     * <p>Create builds and validates its source graph with {@code getTheoreticalSpeed()}. The
     * effective value returned here is what machine ticks, contraptions, movement and renderers
     * use, so a gear chain can still propagate normally while the player's RPM limit remains a
     * hard ceiling on actual work.
     */
    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$capEffectiveSpeed(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        cir.setReturnValue(RpmLimitManager.clamp(self, cir.getReturnValueF()));
    }

    @Inject(method = "initialize", at = @At("TAIL"), remap = false)
    private void dimensionworks$loadMachineLimit(CallbackInfo ci) {
        RpmLimitManager.onMachineLoaded((KineticBlockEntity) (Object) this);
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void dimensionworks$writeLimitData(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        RpmLimitManager.writeSyncData((KineticBlockEntity) (Object) this, compound);
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void dimensionworks$readLimitData(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        RpmLimitManager.readSyncData((KineticBlockEntity) (Object) this, compound);
    }

    /**
     * The extra batches cost extra stress, otherwise over-speeding would be free throughput.
     */
    @Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$overspeedStress(CallbackInfoReturnable<Float> cir) {
        float multiplier = OverspeedBonus.stressMultiplier((KineticBlockEntity) (Object) this);
        if (multiplier > 1)
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
    }

    /**
     * Appends the overspeed line to the goggles readout, after Create's own lines.
     *
     * <p>The renderer drops the last line again when this method reports "no information" while
     * Create already reported some, so returning true is what keeps the extra line on screen.
     */
    @Inject(method = "addToTooltip", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$overspeedTooltip(List<Component> tooltip, boolean isPlayerSneaking,
                                                 CallbackInfoReturnable<Boolean> cir) {
        double multiplier = OverspeedBonus.multiplierOf((KineticBlockEntity) (Object) this);
        if (multiplier <= 1)
            return;
        if (overStressed)
            return;
        CreateLang.translate("gui.stressometer.overspeed_batch",
                OverspeedBonus.formatMultiplier(multiplier))
            .style(ChatFormatting.AQUA)
            .forGoggles(tooltip);
        cir.setReturnValue(true);
    }
}
