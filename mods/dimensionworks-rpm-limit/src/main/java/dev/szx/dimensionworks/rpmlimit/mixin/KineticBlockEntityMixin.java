package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticPersistence;
import com.simibubi.create.content.kinetics.KineticNetwork;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    /**
     * Keep Create's stored network speed inside this machine's owner's limit. Capping only
     * getSpeed() leaves the raw speed available to gears and downstream propagation, allowing
     * every gear stage to multiply an otherwise hidden over-limit value.
     */
    @ModifyVariable(method = "setSpeed", at = @At("HEAD"), argsOnly = true, remap = false)
    private float dimensionworks$capNetworkSpeed(float requested) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        return RpmLimitManager.clampIndividual(self, requested);
    }

    @Redirect(method = "initialize", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/KineticNetwork;initFromTE(FFI)V"), remap = false)
    private void dimensionworks$restoreAccounting(KineticNetwork network, float capacity, float stress, int count) {
        KineticPersistence.initialize((KineticBlockEntity) (Object) this, network);
    }

    @Inject(method = "initialize", at = @At("TAIL"), remap = false)
    private void dimensionworks$loadMachineLimit(CallbackInfo ci) {
        RpmLimitManager.onMachineLoaded((KineticBlockEntity) (Object) this);
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void dimensionworks$writeLimitData(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        RpmLimitManager.writeSyncData((KineticBlockEntity) (Object) this, compound);
        compound.putInt("DWNetworkRpmLimit", ((KineticBlockEntity) (Object) this).getPersistentData().getInt("DWNetworkRpmLimit"));
        KineticPersistence.write((KineticBlockEntity) (Object) this, compound, clientPacket);
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void dimensionworks$readLimitData(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        RpmLimitManager.readSyncData((KineticBlockEntity) (Object) this, compound);
        ((KineticBlockEntity) (Object) this).getPersistentData().putInt("DWNetworkRpmLimit", compound.getInt("DWNetworkRpmLimit"));
        KineticPersistence.read((KineticBlockEntity) (Object) this, compound, clientPacket);
    }

    @Inject(method = "updateFromNetwork", at = @At("RETURN"), remap = false)
    private void dimensionworks$syncNetworkLimit(float capacity, float stress, int count, CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) return;
        int limit = dev.szx.dimensionworks.rpmlimit.optimizer.NetworkRpmLimit.limit(self);
        int previous = self.getPersistentData().getInt("DWNetworkRpmLimit");
        if (previous != limit) {
            float oldSpeed = overStressed ? 0 : dev.szx.dimensionworks.rpmlimit.RpmLimitMath.cap(
                self.getTheoreticalSpeed(), previous > 0 ? previous : RpmLimitManager.individualLimit(self));
            self.getPersistentData().putInt("DWNetworkRpmLimit", limit);
            if (oldSpeed != self.getSpeed()) self.onSpeedChanged(oldSpeed);
            self.setChanged();
            self.sendData();
        }
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

    @Inject(method = "setSpeed", at = @At("RETURN"), remap = false)
    private void dimensionworks$updateStressDelta(float speed, CallbackInfo ci) {
        KineticNetworkOptimizer.onMemberSpeedChanged((KineticBlockEntity) (Object) this);
    }
}
