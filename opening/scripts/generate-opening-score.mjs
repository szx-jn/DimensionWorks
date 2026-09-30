import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const sampleRate = 22050;
const duration = 34.4;
const samples = Math.floor(sampleRate * duration);
const outputPath = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '../public/dw/opening-score.wav',
);

let seed = 0x4d3a2f19;
const noise = () => {
  seed = (seed * 1664525 + 1013904223) >>> 0;
  return (seed / 0xffffffff) * 2 - 1;
};

const sceneHits = [0, 6.7, 14.7, 22.7, 29.4, 32.8];
const pingHits = [6.7, 14.7, 22.7, 29.4];

const expEnv = (t, start, attack, decay) => {
  const x = t - start;
  if (x < 0 || x > attack + decay) {
    return 0;
  }
  if (x < attack) {
    return x / attack;
  }
  return Math.exp(-(x - attack) / decay);
};

const data = Buffer.alloc(samples * 2);
for (let i = 0; i < samples; i++) {
  const t = i / sampleRate;
  const masterFade = Math.min(1, t / 1.3) * Math.min(1, (duration - t) / 2.4);
  const tension = t < 7 ? 1 : t < 15 ? 0.72 : t < 23 ? 0.62 : 0.5;
  const lowDrone =
    Math.sin(Math.PI * 2 * 55 * t) * 0.075 +
    Math.sin(Math.PI * 2 * 82.5 * t) * 0.025;
  const drift = Math.sin(Math.PI * 2 * 0.12 * t) * 0.5 + 0.5;
  let pulse = 0;
  for (let beat = 0; beat < duration; beat += 2) {
    pulse +=
      expEnv(t, beat, 0.012, 0.34) *
      (Math.sin(Math.PI * 2 * 110 * t) * 0.13 +
        Math.sin(Math.PI * 2 * 220 * t) * 0.045);
  }
  let hits = 0;
  for (const hit of sceneHits) {
    hits += expEnv(t, hit, 0.005, 0.42) * noise() * 0.24;
  }
  let pings = 0;
  for (const ping of pingHits) {
    pings +=
      expEnv(t, ping, 0.008, 1.25) *
      (Math.sin(Math.PI * 2 * 523.25 * t) * 0.045 +
        Math.sin(Math.PI * 2 * 784.88 * t) * 0.022);
  }
  const machineNoise = noise() * 0.018 * (0.4 + drift);
  const titleLift =
    t > 24
      ? Math.sin(Math.PI * 2 * 164.81 * t) * 0.024 * Math.min(1, (t - 24) / 2)
      : 0;
  const sample =
    (lowDrone * tension + pulse + hits + pings + machineNoise + titleLift) *
    masterFade;
  data.writeInt16LE(
    Math.max(-32767, Math.min(32767, Math.round(sample * 32767))),
    i * 2,
  );
}

const header = Buffer.alloc(44);
header.write('RIFF', 0);
header.writeUInt32LE(36 + data.length, 4);
header.write('WAVE', 8);
header.write('fmt ', 12);
header.writeUInt32LE(16, 16);
header.writeUInt16LE(1, 20);
header.writeUInt16LE(1, 22);
header.writeUInt32LE(sampleRate, 24);
header.writeUInt32LE(sampleRate * 2, 28);
header.writeUInt16LE(2, 32);
header.writeUInt16LE(16, 34);
header.write('data', 36);
header.writeUInt32LE(data.length, 40);

fs.mkdirSync(path.dirname(outputPath), {recursive: true});
fs.writeFileSync(outputPath, Buffer.concat([header, data]));
console.log(`Wrote ${outputPath}`);
