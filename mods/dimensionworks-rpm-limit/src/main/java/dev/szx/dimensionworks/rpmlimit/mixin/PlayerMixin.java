package dev.szx.dimensionworks.rpmlimit.mixin;

import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Doubles hunger exhaustion at night while the unrepaired night-hunger curse is active. */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float dimensionworks$doubleNightHunger(float amount) {
        if ((Object) this instanceof ServerPlayer player && GearHeartEffects.isNightHungerActive(player)) {
            GearHeartEffects.notifyNightHunger(player);
            return amount * 2.0f;
        }
        return amount;
    }
}
