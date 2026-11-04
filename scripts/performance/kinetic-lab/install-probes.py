"""Install diagnostic-only probes into disposable copies, never production sources."""
from pathlib import Path
import json,shutil,sys
root=Path(sys.argv[1]).resolve()
if root.is_relative_to(Path(__file__).resolve().parents[3]): raise SystemExit('Use an isolated directory outside the production repository')
src=Path(__file__).parent
rpm=root/'dimensionworks-rpm-limit'; mek=root/'dimensionworks-mek-stress'
for name,folder in [('TickMetrics.java','lab'),('LabMetrics.java','lab'),('NetworkMetricsMixin.java','mixin/lab'),('MemberMetricsMixin.java','mixin/lab')]:
 dst=rpm/'src/main/java/dev/szx/dimensionworks/rpmlimit'/folder/name
 dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src/name,dst)
p=rpm/'src/main/resources/dimensionworks_rpm_limit.mixins.json'; j=json.loads(p.read_text())
for name in ['lab.NetworkMetricsMixin','lab.MemberMetricsMixin']:
 if name not in j['mixins']: j['mixins'].append(name)
p.write_text(json.dumps(j,indent=2)+'\n')
metric='dev.szx.dimensionworks.rpmlimit.lab.LabMetrics'
def instrument(p,needle,counter):
 text=p.read_text(); addition=f' if ({metric}.active) {metric}.{counter}++;'
 if needle+addition not in text:
  assert text.count(needle)==1,(p,needle)
  p.write_text(text.replace(needle,needle+addition))
def instrument_if_present(p,needle,counter):
 text=p.read_text()
 if needle in text: instrument(p,needle,counter)
instrument(rpm/'src/main/java/dev/szx/dimensionworks/rpmlimit/optimizer/KineticNetworkOptimizer.java','public static void beginStressRebuild(KineticNetwork network) {','rebuild')
p=rpm/'src/main/java/dev/szx/dimensionworks/rpmlimit/quota/CreateCapacityRegistry.java'
instrument_if_present(p,'for (Map<UUID, Double> contributions : SAMPLES.values()) {','quotaSamples')
instrument_if_present(p,'Map.Entry<KineticNetwork, Map<UUID, Double>> entry = iterator.next();','quotaCleanupSamples')
instrument_if_present(p,'for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {','quotaOwners')
instrument(p,'for (KineticBlockEntity source : network.sources.keySet()) {','quotaSources')
p=mek/'src/main/java/dev/szx/dimensionworks/mekstress/memory/MemoryGridService.java'
instrument(p,'if (refreshing) return lastSnapshot;','snapshotReentries')
instrument(p,'for (IGridNode node : List.copyOf(participants)) {','participants')
if (src/'KineticLabGameTests.java').exists():
 dst=mek/'src/gameTest/java/dev/szx/dimensionworks/mekstress/KineticLabGameTests.java'; shutil.copy2(src/'KineticLabGameTests.java',dst)
 dst=mek/'src/gameTest/resources/data/kineticlab/structures/empty.nbt'; dst.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(mek/'src/gameTest/resources/data/dimensionworks_mek_stress/structures/empty.nbt',dst)
 p=mek/'build.gradle'; t=p.read_text(); extra="\nminecraft.runs.gameTestServer.property 'forge.enabledGameTestNamespaces', 'kineticlab'\n"
 if extra not in t:p.write_text(t+extra)
