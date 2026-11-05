package dev.szx.dimensionworks.transfer.tutorial;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
public final class TutorialSavedData extends SavedData {
 private final Map<UUID,CompoundTag> states=new HashMap<>();
 public static TutorialSavedData get(ServerPlayer p){return p.server.overworld().getDataStorage().computeIfAbsent(TutorialSavedData::load,TutorialSavedData::new,"dw_dispatch_tutorial");}
 public CompoundTag get(UUID id){return states.get(id);}
 public void put(UUID id,CompoundTag tag){states.put(id,tag.copy());setDirty();}
 public static TutorialSavedData load(CompoundTag root){TutorialSavedData d=new TutorialSavedData();for(String k:root.getAllKeys())try{d.states.put(UUID.fromString(k),root.getCompound(k).copy());}catch(IllegalArgumentException ignored){}return d;}
 @Override public CompoundTag save(CompoundTag root){states.forEach((id,tag)->root.put(id.toString(),tag.copy()));return root;}
}
