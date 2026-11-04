"""Extract exact call counts and timing samples; timing is not a production TPS claim."""
import csv,json,statistics,sys
from pathlib import Path
out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
cols='run scenario n s u rep wall_ns cpu_ns sync member_updates actual_stress actual_capacity stress_rebuilds me_snapshots me_participants quota_samples quota_owners quota_cleanup_samples quota_sources'.split()
rows=[];ticks=[];status={}
for arg in sys.argv[2:]:
 p=Path(arg);text=p.read_text(errors='replace'); status[p.stem]={'build_success': 'BUILD SUCCESSFUL' in text,'gametest_complete':'GAME TESTS COMPLETE' in text,'required_failure':'required tests failed' in text}
 for line in text.splitlines():
  if line.startswith('KINETIC_LAB,') and ',scenario,' not in line:
   r=next(csv.reader([line]));rows.append(dict(zip(cols,[p.stem]+r[1:])))
  elif line.startswith('KINETIC_TICK,'):
   r=next(csv.reader([line]));ticks.append([p.stem]+r[1:])
with (out/'samples.csv').open('w') as f:
 w=csv.DictWriter(f,fieldnames=cols);w.writeheader();w.writerows(rows)
with (out/'ticks.csv').open('w') as f:
 w=csv.writer(f);w.writerow(['run','n','remaining','wall_ns','cpu_ns']+cols[8:]);w.writerows(ticks)
summary=[]
for key in sorted(set((r['scenario'],int(r['n']),int(r['s']),int(r['u'])) for r in rows)):
 rr=[r for r in rows if (r['scenario'],int(r['n']),int(r['s']),int(r['u']))==key and int(r['rep'])>=0]
 item=dict(zip(['scenario','n','s','u'],key));item['samples']=len(rr)
 for k in ['wall_ns','cpu_ns']:
  item['median_'+k]=int(statistics.median(int(r[k]) for r in rr))
  item['max_'+k]=max(int(r[k]) for r in rr)
 for k in cols[8:]: item[k]=sorted(set(int(r[k]) for r in rr))
 summary.append(item)
(out/'summary.json').write_text(json.dumps({'runs':status,'summary':summary},indent=2)+'\n')
