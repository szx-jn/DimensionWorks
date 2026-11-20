package dev.szx.dimensionworks.transfer.compat;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
/** Typed interface to the pack's required, fixed CMP fork. */
public final class PlayerParcel {
    private PlayerParcel() {}
    public static boolean isPacked(Player player){return PackedPlayerState.isPacked(player);}
    public static boolean unpack(ServerPlayer player){
        if(isPacked(player))PackedPlayerState.setPacked(player,false,false);
        return !isPacked(player);
    }
}
