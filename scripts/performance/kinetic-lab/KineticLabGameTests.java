package dev.szx.dimensionworks.mekstress;

import java.util.*;
import java.lang.management.ManagementFactory;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.rpmlimit.lab.LabMetrics;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("kineticlab") @PrefixGameTestTemplate(false)
public class KineticLabGameTests {
 static volatile float blackhole;
 static GameTestHelper h;
 static int serial;
 static final List<BlockPos> placed=new ArrayList<>();
 static net.minecraft.world.level.block.entity.BlockEntity place(String id,int x,int y,int z) {
  BlockPos p=h.absolutePos(new BlockPos(x,y,z));
  h.getLevel().setChunkForced(p.getX()>>4,p.getZ()>>4,true); h.getLevel().getChunkAt(p); // setup only, not a production loading policy
  h.getLevel().setBlock(p,ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState(),18);
  placed.add(p); return h.getLevel().getBlockEntity(p);
 }
 record Fixture(KineticNetwork network,List<KineticBlockEntity> members) {}
 static Fixture fixture(int n,int sources,int offset) {
  List<KineticBlockEntity> ms=new ArrayList<>();
  long id=h.absolutePos(new BlockPos(offset,90,0)).asLong();
  for(int i=0;i<n;i++) {
   KineticBlockEntity be=(KineticBlockEntity)place(i<sources?"create:creative_motor":"create:shaft",offset+(i%64)*2,90,(i/64)*2);
   be.setSpeed(16); be.setNetwork(id); ms.add(be);
  }
  KineticNetwork net=ms.get(0).getOrCreateNetwork();
  for(KineticBlockEntity be:ms) net.members.put(be,1f);
  KineticNetworkOptimizer.invalidate(net); net.updateNetwork();
  h.assertTrue(net.members.size()==n,"Network membership mismatch "+net.members.size()+" vs "+n);
  return new Fixture(net,ms);
 }
 static void remove(Fixture f) {
  for(var be:f.members) be.setNetwork(null);
  f.network.members.clear(); f.network.sources.clear();
  CreateCapacityRegistry.clearRemoved();
  for(var be:f.members) h.getLevel().setBlock(be.getBlockPos(),Blocks.AIR.defaultBlockState(),18);
 }
 static void sample(String scenario,int n,int s,int u,int rep,Runnable action) {
  LabMetrics.reset(); LabMetrics.active=true;
  long cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(), start=System.nanoTime();
  try { action.run(); } finally { LabMetrics.active=false; }
  long nanos=System.nanoTime()-start;
  long cpuNanos=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu;
  System.out.println("KINETIC_LAB,"+scenario+","+n+","+s+","+u+","+rep+","+nanos+","+cpuNanos+","+LabMetrics.values());
 }
 static void accountSuite() {
  for(int n:new int[]{128,1024,4096}) {
   Fixture f=fixture(n,1,0); var net=f.network; var target=f.members.get(n-1);
   for(int rep=-1;rep<5;rep++) {
    sample("unchanged",n,1,100,rep,()->{ for(int i=0;i<100;i++) net.updateStressFor(target,net.members.get(target)); });
    sample("sequential_changes",n,1,100,rep,()->{ for(int i=0;i<100;i++) net.updateStressFor(target,net.members.get(target)+1); });
    sample("batched_changes",n,1,100,rep,()->{KineticNetworkOptimizer.beginBatch();try {for(int i=0;i<100;i++)net.updateStressFor(target,net.members.get(target)+1);}finally{KineticNetworkOptimizer.endBatch();}});
    sample("invalidate_only",n,1,1000,rep,()->{for(int i=0;i<1000;i++)KineticNetworkOptimizer.invalidate(net);});
    net.updateNetwork();
    float exact=0;for(var be:net.members.keySet())exact+=net.getActualStressOf(be);
    h.assertTrue(Math.abs(exact-net.calculateStress())<.5f,"Delta cache differs from exact stress");
    sample("equal_invalidate_refresh",n,1,10,rep,()->{for(int i=0;i<10;i++){KineticNetworkOptimizer.invalidate(net);net.updateNetwork();}});
   }
   remove(f);
  }
  for(int s:new int[]{1,16,128,512}) {
   Fixture f=fixture(Math.max(128,s),s,0); var net=f.network;
   for(int rep=-1;rep<5;rep++)sample("cached_capacity_reads",f.members.size(),s,100,rep,()->{for(int i=0;i<100;i++) blackhole=net.calculateCapacity();});
   remove(f);
  }
  for(int g:new int[]{1,16,64}) {
   List<Fixture> fs=new ArrayList<>();for(int i=0;i<g;i++)fs.add(fixture(1,1,i*3));
   for(int rep=-1;rep<5;rep++)sample("global_capacity_reads",g,1,g,rep,()->{for(var f:fs)blackhole=f.network.calculateCapacity();});
   for(var f:fs)remove(f);
  }
 }
 static void physicalSuite(int index) {
  int[] sizes={128,512,1024};
  if(index==sizes.length){meSuite(0);return;}
  int n=sizes[index],z=300+index*8;
  var level=h.getLevel();
  for(int i=1;i<n;i++){
   BlockPos p=h.absolutePos(new BlockPos(i,90,z));level.setChunkForced(p.getX()>>4,p.getZ()>>4,true);level.getChunkAt(p);
   level.setBlock(p,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:shaft")).defaultBlockState().setValue(com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock.AXIS,net.minecraft.core.Direction.Axis.X),18);
  }
  BlockPos pos=h.absolutePos(new BlockPos(0,90,z));level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);level.getChunkAt(pos);
  level.setBlock(pos,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:creative_motor")).defaultBlockState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,net.minecraft.core.Direction.EAST),3);
  h.runAfterDelay(40,()->{
   var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(pos);
   var net=motor.getOrCreateNetwork();
   h.assertTrue(net.members.size()==n,"Physical chain must be connected: "+net.members.size()+"/"+n);
   for(int rep=-1;rep<5;rep++){
    sample("physical_speed_change",n,1,1,rep,()->motor.generatedSpeed.setValue(motor.generatedSpeed.getValue()==16?32:16));
   }
   dev.szx.dimensionworks.rpmlimit.lab.TickMetrics.n=n;
   dev.szx.dimensionworks.rpmlimit.lab.TickMetrics.remaining=100;
   h.runAfterDelay(110,()->{
    sample("physical_remove_source",n,1,1,0,()->level.setBlock(pos,Blocks.AIR.defaultBlockState(),3));
    for(int i=1;i<n;i++)level.setBlock(h.absolutePos(new BlockPos(i,90,z)),Blocks.AIR.defaultBlockState(),18);
    physicalSuite(index+1);
   });
  });
 }
 static void meSuite(int index) {
  int[] sizes={8,32,128};
  if(index==sizes.length) {h.succeed();return;}
  int p=sizes[index],z=160+index*4;
  MemoryDriveBlockEntity drive=(MemoryDriveBlockEntity)place("dimensionworks_mek_stress:memory_drive_ddr1",0,90,z);
  place("ae2:creative_energy_cell",-1,90,z);
  List<MEGearboxBlockEntity> boxes=new ArrayList<>();
  for(int i=0;i<p;i++){
   MEGearboxBlockEntity box=(MEGearboxBlockEntity)place("appliedcreate:me_gearbox",i+1,90,z);
   box.setMode(MEGearboxBlockEntity.Mode.EXPORT);box.setConfiguredStress(0);boxes.add(box);
  }
  h.runAfterDelay(60,()->{
   h.assertTrue(drive.getMainNode().isActive(),"ME drive must be active");
   for(var box:boxes) h.assertTrue(box.getMainNode().getGrid()==drive.getMainNode().getGrid(),"ME fixture disconnected");
   IMemoryGridService service=drive.getMainNode().getGrid().getService(IMemoryGridService.class);
   for(int rep=-1;rep<5;rep++) {
    sample("me_snapshot_reads",p+1,0,100,rep,()->{for(int i=0;i<100;i++) service.snapshot(h.getLevel().getGameTime());});
    sample("me_generated_speed_reads",p+1,0,p,rep,()->{for(var box:boxes)blackhole=box.getGeneratedSpeed();});
   }
   meSuite(index+1);
  });
 }
 @GameTest(template="empty",timeoutTicks=1200)
 public static void benchmarks(GameTestHelper helper) {
  h=helper;
  dev.szx.dimensionworks.rpmlimit.RpmLimitConfig.DEFAULT_RPM.set(10240);
  System.out.println("KINETIC_LAB,scenario,n,s,u,rep,wall_ns,cpu_ns,sync,member_updates,actual_stress,actual_capacity,stress_rebuilds,me_snapshots,me_participants,quota_samples,quota_owners,quota_cleanup_samples,quota_sources");
  h.runAfterDelay(20,()->{accountSuite();physicalSuite(0);});
 }
}
