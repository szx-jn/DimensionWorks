package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket", remap = false)
public abstract class ElasticPlayerPackingMixin {
    // This point is reached only after CMP has accepted immunity, range and packed-state checks.
    @Inject(method = "pack", at = @At(value = "INVOKE", target =
        "Lde/theidler/create_mobile_packages/items/mobile_packager/PackedPlayerState;setPacked(Lnet/minecraft/server/level/ServerPlayer;ZZ)V"), remap = false)
    private static void dimensionworks$selectElasticTool(ServerPlayer source, Player target, boolean self, CallbackInfo ci) {
        ElasticParcelState.selectTool(source, target);
    }
}
