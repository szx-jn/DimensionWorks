package dev.szx.dimensionworks.rpmlimit.lab;
public final class LabMetrics {
 public static boolean active;
 public static long sync, members, stress, capacity, rebuild, snapshots, participants;
 public static long quotaSamples, quotaOwners, quotaCleanupSamples, quotaSources;
 public static void reset() {
  sync=members=stress=capacity=rebuild=snapshots=participants=0;
  quotaSamples=quotaOwners=quotaCleanupSamples=quotaSources=0;
 }
 public static String values() {
  return sync+","+members+","+stress+","+capacity+","+rebuild+","+snapshots+","+participants
   +","+quotaSamples+","+quotaOwners+","+quotaCleanupSamples+","+quotaSources;
 }
}
