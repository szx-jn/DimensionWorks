import shots from './shots.json';
import {Vec3} from './motion';

export type Layout = 'vista' | 'center' | 'assembly' | 'split' | 'overhead' | 'matrix' | 'closeup';
export type ScenePlan = {
  layout: Layout; camera: Vec3; target: Vec3; dolly: Vec3;
  accent: number; motif: 'shaft' | 'ring' | 'circuit' | 'route';
};

// Shot-specific framing. No left-text/right-picture template is used.
const layouts: Layout[] = [
  'vista','closeup','vista','vista','vista',
  'center','split','matrix','assembly','center','overhead',
  'assembly','vista','center','closeup','matrix','assembly','overhead',
  'assembly','split','assembly','matrix','matrix','assembly','split',
  'overhead','matrix','assembly','overhead','split','center','assembly',
  'matrix','split','closeup','split','matrix','assembly','closeup',
  'matrix','assembly','overhead','center','matrix','vista','closeup',
  'center','assembly','matrix','assembly',
  'vista','center','split','matrix',
  'assembly','center','matrix','closeup','overhead','vista',
];
const cameras: Record<Layout, Vec3> = {
  vista: [9, 6.5, 12], center: [7.5, 6, 12.5], assembly: [6.5, 6.5, 12.5],
  split: [1, 7.8, 14.8], overhead: [2.5, 13.8, 9], matrix: [0.6, 6.8, 15.2],
  closeup: [4.1, 5.4, 9.1],
};
export const scenePlans: ScenePlan[] = shots.map((shot, index) => ({
  layout: layouts[index],
  camera: cameras[layouts[index]],
  target: [0, 0.85, 0],
  dolly: index % 2 ? [-0.9, 0.2, -0.7] : [0.7, -0.4, -0.6],
  accent: 32 + index % 5 * 4,
  motif: (['shaft', 'ring', 'circuit', 'route'] as const)[index % 4],
}));

export const shotEvents = (shotId: number) => {
  const frames = Math.round(shots[shotId].duration * 30);
  // These births drive real, visible copper packets in ScenePrimitives.TracePackets.
  const events = [0, 4, 8, 12, scenePlans[shotId].accent];
  for (let at = 12; at < frames - 10; at += 53) events.push(at);
  if (shotId === 0) events.push(28, 56, 84);
  return [...new Set(events)].sort((a, b) => a - b);
};
