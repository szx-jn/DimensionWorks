package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerSyncClient", remap = false)
public abstract class PackedPlayerSyncMixin {
    @Inject(method = "handle", at = @At("TAIL"), remap = false)
    private static void dimensionworks$syncParcel(int id, boolean packed, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(id) instanceof Player player)) return;
        PackedPlayerTransport.synchronize(player);
        if (!packed && player == mc.player) {
            com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler.ridingChainConveyor = null;
            com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler.ridingConnection = null;
        }
    }
}
