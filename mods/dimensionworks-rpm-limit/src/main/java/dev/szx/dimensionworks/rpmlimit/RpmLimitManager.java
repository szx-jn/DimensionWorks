package dev.szx.dimensionworks.rpmlimit;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity; import net.minecraft.nbt.CompoundTag; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.entity.player.Player; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class RpmLimitManager { private static final String PLAYER_KEY="dimensionworks_rpm_limit",OWNER_KEY="dimensionworks_owner",INIT_KEY="dimensionworks_rpm_initialized"; private static final Map<UUID,Integer>CACHE=new ConcurrentHashMap<>(); private static long last=Long.MIN_VALUE;
public static void init(Player p){CompoundTag d=p.getPersistentData();if(!d.getBoolean(INIT_KEY)){d.putInt(PLAYER_KEY,RpmLimitConfig.DEFAULT_RPM.get());d.putBoolean(INIT_KEY,true);}CACHE.put(p.getUUID(),sanitize(d.getInt(PLAYER_KEY)));}
public static int get(UUID id){return CACHE.getOrDefault(id,RpmLimitConfig.DEFAULT_RPM.get());}
public static int getLimit(UUID id){return get(id);}
public static void setLimit(ServerPlayer p,int rpm){int v=sanitize(rpm);p.getPersistentData().putInt(PLAYER_KEY,v);p.getPersistentData().putBoolean(INIT_KEY,true);CACHE.put(p.getUUID(),v);}
public static void refresh(Iterable<ServerPlayer> ps,long tick){int i=Math.max(1,RpmLimitConfig.CACHE_INTERVAL_TICKS.get());if(tick-last<i)return;last=tick;for(ServerPlayer p:ps){int v=sanitize(p.getPersistentData().getInt(PLAYER_KEY));p.getPersistentData().putInt(PLAYER_KEY,v);CACHE.put(p.getUUID(),v);}}
public static float clamp(KineticBlockEntity k,float requested){if(k.getLevel()==null||k.getLevel().isClientSide)return requested;UUID o=owner(k,32);if(o==null)return requested;int l=get(o);return Math.abs(requested)>l?Math.copySign(l,requested):requested;}
public static UUID owner(KineticBlockEntity k,int depth){CompoundTag d=k.getPersistentData();UUID own=d.hasUUID(OWNER_KEY)?d.getUUID(OWNER_KEY):null;if(k.hasSource()&&depth>0&&k.getLevel()!=null){var be=k.getLevel().getBlockEntity(k.source);if(be instanceof KineticBlockEntity s){UUID o=owner(s,depth-1);if(o!=null)return o;}}return own;}
public static void setOwner(KineticBlockEntity k,UUID u){k.getPersistentData().putUUID(OWNER_KEY,u);k.setChanged();}
private static int sanitize(int v){return Math.max(1,Math.min(v,Math.max(1,RpmLimitConfig.MAX_RPM.get())));}}
