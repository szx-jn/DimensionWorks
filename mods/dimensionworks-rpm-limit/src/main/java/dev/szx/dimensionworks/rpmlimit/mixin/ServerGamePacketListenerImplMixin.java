package dev.szx.dimensionworks.rpmlimit.mixin;

import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks player packets while the cursed freeze is active. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    private boolean dimensionworks$isFrozen() {
        return GearHeartEffects.isFrozen(dimensionworks$player());
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeMovement(CallbackInfo ci) {
        if (dimensionworks$isFrozen()) {
            GearHeartEffects.enforceFreeze(dimensionworks$player());
            ci.cancel();
        }
    }

    @Unique
    private net.minecraft.server.level.ServerPlayer dimensionworks$player() {
        return ((ServerGamePacketListenerImplAccessor) (Object) this).dimensionworks$getPlayer();
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezePlayerAction(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeUseOn(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeUse(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeInteract(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeHotbar(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeCommand(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeContainerClick(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeCreativeSlot(CallbackInfo ci) {
        if (dimensionworks$isFrozen())
            ci.cancel();
    }
}
