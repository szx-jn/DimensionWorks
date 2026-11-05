package dev.szx.dimensionworks.transfer.compat;
import dev.szx.dimensionworks.transfer.WDTransfer;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
public final class PlayerParcel {
 private static Method method; private static boolean looked;
 private PlayerParcel(){}
 public static boolean isPacked(Player p){return p.getPersistentData().getBoolean("cmp_packed_player");}
 public static boolean unpack(ServerPlayer p){if(!isPacked(p))return true;try{if(!looked){looked=true;method=Class.forName("de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState").getMethod("setPacked",ServerPlayer.class,boolean.class,boolean.class);} if(method==null)return false;method.invoke(null,p,false,false);return !isPacked(p);}catch(ReflectiveOperationException e){WDTransfer.LOGGER.error("Mobile Packages unpack failed",e);return false;}}
}
