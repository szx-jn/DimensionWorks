import {Easing, interpolate, spring} from 'remotion';

export const FPS = 30;
export const STAGGER = 4;
export const ENTER_FRAMES = 24;
export const EXIT_FRAMES = 10;
export const TRACE_INTERVAL = 53; // 1.7667 s: packets enter independently of machine cycles.

export const eased = (
  frame: number,
  input: number[],
  output: number[],
  easing = Easing.inOut(Easing.cubic),
) => interpolate(frame, input, output, {
  easing,
  extrapolateLeft: 'clamp',
  extrapolateRight: 'clamp',
});

export const arrive = (frame: number, delay = 0) => frame < delay ? 0 : spring({
  fps: FPS,
  frame: frame - delay,
  durationInFrames: ENTER_FRAMES,
  config: {damping: 19, stiffness: 155, mass: 0.8},
});

export const leave = (frame: number, duration: number) => 1 - eased(
  frame, [duration - EXIT_FRAMES, duration - 1], [0, 1], Easing.in(Easing.cubic),
);

export const impact = (frame: number, at: number) => eased(
  frame, [at, at + 2, at + 6], [0, 1, 0], Easing.out(Easing.cubic),
);

export const phase = (frame: number, period: number, offset = 0) =>
  (frame / FPS + offset) * Math.PI * 2 / period;

export type Vec3 = [number, number, number];
export const blend3 = (a: Vec3, b: Vec3, t: number): Vec3 =>
  [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
