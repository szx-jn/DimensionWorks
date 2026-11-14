package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.elastic.ElasticVerticalBelt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ElasticVerticalBeltMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$verticalBelt(Vec3 input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && player.isControlledByLocalInstance()
            && ElasticVerticalBelt.travel(player, input)) ci.cancel();
    }
}
