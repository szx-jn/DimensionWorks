package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.handler.UnpackFireworks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts the shared effect exactly when CMP changes a packed player back to unpacked. */
@Mixin(targets = "de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState", remap = false)
@Pseudo
abstract class PackedPlayerStateMixin {
    @Inject(method = "setPacked", at = @At("TAIL"), remap = false)
    private static void dimensionworks$updatePackedTag(ServerPlayer player, boolean value, boolean byOther, CallbackInfo ci) {
        dev.szx.dimensionworks.transfer.elastic.ElasticParcelState.synchronize(player);
        dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport.synchronize(player);
        if (!value) com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.handleStopRidingPacket(player);
    }

    @Inject(method = "setPacked", at = @At("HEAD"), remap = false)
    private static void dimensionworks$startUnpackFireworks(
        ServerPlayer player,
        boolean value,
        boolean byOther,
        CallbackInfo callback
    ) {
        if (!value) dev.szx.dimensionworks.transfer.elastic.ElasticParcelState.clear(player);
        if (!value && player.getPersistentData().getBoolean("cmp_packed_player")) {
            UnpackFireworks.start(player);
        }
    }
}
