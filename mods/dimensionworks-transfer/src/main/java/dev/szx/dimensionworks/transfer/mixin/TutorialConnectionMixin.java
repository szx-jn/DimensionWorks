package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraftforge.network.NetworkEvent;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TutorialConnectionMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void freezeMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void freezeInventory(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) { player.containerMenu.sendAllDataToRemote(); ci.cancel(); }
    }

    @Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
    private void freezeContainerButtons(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void freezeRecipePlacement(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) { player.containerMenu.sendAllDataToRemote(); ci.cancel(); }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void freezeCreativeInventory(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void freezeCommands(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void freezeHotbar(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void freezeActions(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void freezeCommands(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (TutorialManager.locked(player)) ci.cancel();
    }
}

/** The packer's emergency escape input is also an operation, so the tutorial suppresses it. */
@Mixin(targets = "de.theidler.create_mobile_packages.items.mobile_packager.EscapePackedPlayerPacket", remap = false)
@Pseudo
abstract class TutorialPackedEscapeMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$freezeEscape(NetworkEvent.Context context,
            CallbackInfoReturnable<Boolean> callback) {
        if (context.getSender() != null && TutorialManager.locked(context.getSender())) {
            callback.setReturnValue(true);
        }
    }
}
