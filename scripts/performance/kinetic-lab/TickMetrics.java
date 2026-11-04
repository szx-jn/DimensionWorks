package dev.szx.dimensionworks.rpmlimit.lab;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid="dimensionworks_rpm_limit")
public final class TickMetrics {
 public static int remaining,n;
 private static long start,cpu;
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
  if(remaining<=0)return;
  if(e.phase==TickEvent.Phase.START){LabMetrics.reset();LabMetrics.active=true;start=System.nanoTime();cpu=java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();}
  else if(start!=0){LabMetrics.active=false;System.out.println("KINETIC_TICK,"+n+","+remaining+","+(System.nanoTime()-start)+","+(java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)+","+LabMetrics.values());remaining--;start=0;}
 }
}
