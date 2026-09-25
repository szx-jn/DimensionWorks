package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tags pre-existing speed controllers when a player changes their value, so worlds created
 * before ownership tracking still get player limits applied to those controllers.
 */
@Mixin(value = ValueSettingsPacket.class, remap = false)
public abstract class ValueSettingsPacketMixin {

    @Inject(
        method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;)V",
        at = @At("HEAD"),
        remap = false
    )
    private void dimensionworks$assignControllerOwner(ServerPlayer player, SmartBlockEntity be, CallbackInfo ci) {
        if (be instanceof SpeedControllerBlockEntity controller)
            RpmLimitManager.setOwner(controller, player.getUUID());
    }
}
