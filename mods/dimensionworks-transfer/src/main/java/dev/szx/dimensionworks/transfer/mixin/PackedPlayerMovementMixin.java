package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PackedPlayerMovementMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$parcelPush(Entity other, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if ((entity instanceof Player player && dev.szx.dimensionworks.transfer.compat.PlayerParcel.isPacked(player))
            || (other instanceof Player otherPlayer && dev.szx.dimensionworks.transfer.compat.PlayerParcel.isPacked(otherPlayer))) ci.cancel();
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$moveAsChainParcel(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player && PackedPlayerTransport.ignoresBlocks(player)) {
            // Match Entity's noPhysics movement branch without leaving a mutable noPhysics flag behind.
            entity.setPos(entity.getX() + movement.x, entity.getY() + movement.y, entity.getZ() + movement.z);
            ci.cancel();
        }
    }
}
