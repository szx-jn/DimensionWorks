package dev.szx.dimensionworks.transfer.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.elastic.ElasticPackager;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager", remap = false)
public abstract class ElasticToolInteractionMixin {
    // This Item override is SRG-named in the fixed CMP release JAR.
    @Redirect(method = {"interactLivingEntity", "m_6880_"}, at = @At(value = "INVOKE", target =
        "Lde/theidler/create_mobile_packages/items/mobile_packager/PackPlayerPacket;pack(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Player;Z)V"), remap = false)
    private void dimensionworks$useActualInteractionTool(ServerPlayer source, Player target, boolean self) {
        boolean wasPacked = PlayerParcel.isPacked(target);
        PackPlayerPacket.pack(source, target, self);
        boolean elastic = (Object) this instanceof ElasticPackager;
        if (!wasPacked && target instanceof ServerPlayer player && PlayerParcel.isPacked(player)
            && ElasticParcelState.isElastic(player) != elastic) {
            ElasticParcelState.setElastic(player, elastic);
            ElasticParcelState.synchronize(player);
        }
    }
}
