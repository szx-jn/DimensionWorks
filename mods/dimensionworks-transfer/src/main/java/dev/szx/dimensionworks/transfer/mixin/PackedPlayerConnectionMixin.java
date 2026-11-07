package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PackedPlayerConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$allowChainParcel(LevelReader level, AABB oldBox, double x, double y, double z,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (PackedPlayerTransport.ignoresBlocks(player)) cir.setReturnValue(false);
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void dimensionworks$chainSuspendsParcel(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (PackedPlayerTransport.ignoresBlocks(player)) {
            clientIsFloating = false;
            player.fallDistance = 0;
        }
    }
}
