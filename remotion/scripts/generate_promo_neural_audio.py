#!/usr/bin/env python3
"""Free neural Mandarin speech, measured subtitles and an original film score.

Edge requests are sequential and cached. Speech stays at its natural rate;
the edit allocates real speaking time instead of accelerating the recording.
Run before the full video render to keep memory usage bounded.
"""
import argparse
import asyncio
import hashlib
import json
import math
import subprocess
import tempfile
import wave
from pathlib import Path

import edge_tts
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'public/promo/audio'
SHOTS = ROOT / 'src/promo/shots.json'
SR, TOTAL = 24000, 360


def command(args):
    return subprocess.run(args, check=True, capture_output=True, text=True)


def write_wave(path, samples):
    with wave.open(str(path), 'wb') as stream:
        stream.setnchannels(1 if samples.ndim == 1 else samples.shape[1])
        stream.setsampwidth(2)
        stream.setframerate(SR)
        stream.writeframes((np.clip(samples, -1, 1) * 32767).astype('<i2').tobytes())


async def narrate(shots, voice):
    OUT.mkdir(parents=True, exist_ok=True)
    cache = OUT / 'neural-clips'
    cache.mkdir(exist_ok=True)
    clips = []
    for shot in shots:
        text = shot['voice']
        if not text:
            clips.append(None)
            continue
        spoken = text.replace('Mekanism', '机械工艺').replace('Mek', '机械工艺').replace('Create', '机械动力')
        spoken = spoken.replace('ME', 'M E').replace('AE', 'A E').replace('Boss', '首领')
        digest = hashlib.sha256((voice + '\0' + spoken + '\0+0%').encode()).hexdigest()[:16]
        mp3 = cache / f'{shot["id"]:02d}-{digest}.mp3'
        metadata = mp3.with_suffix('.json')
        if not (mp3.exists() and metadata.exists()):
            boundaries = []
            speech = edge_tts.Communicate(spoken, voice, rate='+0%', boundary='SentenceBoundary',
                                         connect_timeout=15, receive_timeout=45)
            with mp3.open('wb') as stream:
                async for chunk in speech.stream():
                    if chunk['type'] == 'audio':
                        stream.write(chunk['data'])
                    elif chunk['type'] == 'SentenceBoundary':
                        boundaries.append({key: chunk[key] for key in ('offset', 'duration', 'text')})
            if not boundaries or mp3.stat().st_size == 0:
                raise RuntimeError(f'No neural speech returned for shot {shot["id"]}')
            metadata.write_text(json.dumps(boundaries, ensure_ascii=False, indent=2) + '\n')
        wav = mp3.with_suffix('.wav')
        if not wav.exists():
            command(['ffmpeg', '-v', 'error', '-y', '-i', str(mp3), '-ar', str(SR), '-ac', '1', str(wav)])
        with wave.open(str(wav)) as stream:
            samples = np.frombuffer(stream.readframes(stream.getnframes()), dtype='<i2').astype(np.float32) / 32768
        clips.append({'samples': samples, 'boundaries': json.loads(metadata.read_text()), 'file': mp3.name,
                      'seconds': len(samples) / SR})
        print(f'Neural narration {shot["id"] + 1}/60: {len(samples) / SR:.2f}s / {shot["duration"]}s shot', flush=True)
    return clips


def fit_timeline(shots, clips):
    minimum = [math.ceil((clip['seconds'] + .55) * 2) / 2 if clip else 3 for clip in clips]
    durations = [max(shot['duration'], need) for shot, need in zip(shots, minimum)]
    excess = round(sum(durations) - TOTAL, 3)
    # Supporting overviews yield their spare pauses before the main demonstrations.
    priority = [10, 26, 30, 32, 38, 43, 46, 50, 51, 52, 53, 54, 55, 56, 58, 6, 7, 8, 9, 12, 13, 15, 17, 23,
                27, 29, 31, 33, 36, 37, 39, 41, 42, 45, 47, 48, 49, 57, 0, 1, 3, 4, 59, 11, 14, 16, 18, 19, 20,
                21, 22, 24, 25, 28, 34, 35, 40, 44, 2]
    while excess > 0:
        candidates = [i for i in priority if durations[i] - minimum[i] >= .5]
        if not candidates:
            raise RuntimeError(f'Narration needs {sum(minimum):.1f}s; shorten the working script before rendering')
        for i in candidates:
            durations[i] -= .5
            excess -= .5
            if excess <= 0:
                break
    start = 0
    for shot, seconds in zip(shots, durations):
        shot.update(start=start, duration=seconds)
        start += seconds
    assert start == TOTAL
    SHOTS.write_text(json.dumps(shots, ensure_ascii=False, indent=2) + '\n')


def assemble_narration(shots, clips, voice):
    track = np.zeros(TOTAL * SR, dtype=np.float32)
    cues, manifest = [], []
    for shot, clip in zip(shots, clips):
        if not clip:
            continue
        offset = round((shot['start'] + .25) * SR)
        samples = clip['samples']
        track[offset:offset + len(samples)] = samples
        local_end = 0
        for boundary in clip['boundaries']:
            start = max(local_end, boundary['offset'] / 10000)
            end = min(clip['seconds'] * 1000, (boundary['offset'] + boundary['duration']) / 10000)
            if end <= start:
                raise RuntimeError(f'Invalid sentence timing in shot {shot["id"]}')
            text = boundary['text'].replace('M E', 'ME').replace('A E', 'AE')
            cues.append({'text': text, 'startMs': round((shot['start'] + .25) * 1000 + start),
                         'endMs': round((shot['start'] + .25) * 1000 + end), 'timestampMs': None,
                         'confidence': None, 'shotId': shot['id']})
            local_end = end
        manifest.append({'id': shot['id'], 'voice': voice, 'file': clip['file'], 'text': shot['voice'],
                         'duration': clip['seconds'], 'start': shot['start'] + .25})
    write_wave(OUT / 'narration.wav', track)
    (ROOT / 'src/promo/captions.json').write_text(json.dumps(cues, ensure_ascii=False, indent=2) + '\n')
    (OUT / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')


def score(shots):
    rng = np.random.default_rng(261115)
    music = np.zeros((TOTAL * SR, 2), dtype=np.float32)
    chords = [(110, 130.813, 164.814, 220), (87.307, 110, 130.813, 174.614),
              (98, 123.471, 146.832, 196), (82.407, 110, 146.832, 164.814)]

    def add(time, signal, gain=1, pan=0):
        offset = round(time * SR)
        count = min(len(signal), len(music) - offset)
        if count <= 0:
            return
        music[offset:offset + count, 0] += signal[:count] * gain * (1 - pan * .35)
        music[offset:offset + count, 1] += signal[:count] * gain * (1 + pan * .35)

    for begin in range(0, TOTAL, 8):
        t = np.arange(10 * SR, dtype=np.float32) / SR
        envelope = np.minimum(1, t / 1.7) * np.minimum(1, np.maximum(0, (10 - t) / 2))
        pad = np.zeros_like(t)
        for freq in chords[(begin // 8) % len(chords)]:
            pad += (np.sin(2 * np.pi * freq * t) + .22 * np.sin(2 * np.pi * (freq * 2 + .12) * t)) * .012
        add(begin, pad * envelope, .6 if begin < shots[5]['start'] else 1, .25 * math.sin(begin))
    for begin in np.arange(shots[11]['start'], shots[59]['start'], .625):
        t = np.arange(round(.35 * SR), dtype=np.float32) / SR
        pulse = np.sin(2 * np.pi * (58 * t - 16 * t * t)) * np.exp(-t * 16)
        add(begin, pulse, .026 if begin < shots[39]['start'] else .016, -.2)
        tick = rng.normal(0, 1, len(t)).astype(np.float32) * np.exp(-t * 130)
        add(begin + .3125, tick, .012, .55)
    for begin in np.arange(0, shots[5]['start'], 1.16).tolist() + np.arange(shots[59]['start'], 359, 1.16).tolist():
        t = np.arange(SR, dtype=np.float32) / SR
        heart = np.sin(2 * np.pi * 52 * t) * np.exp(-t * 30)
        add(begin, heart, .15)
        add(begin + .2, heart, .07)
    for frame in (28, 56, 84):
        t = np.arange(round(.45 * SR), dtype=np.float32) / SR
        metal = (np.sin(2*np.pi*740*t) + .4*np.sin(2*np.pi*1131.7*t)) * np.exp(-t*24)
        add(frame / 30, metal, .05, -.35 if frame == 28 else .35)
    for shot in shots:
        start = shot['start']
        t = np.arange(round(.65 * SR), dtype=np.float32) / SR
        air = rng.normal(0, 1, len(t)).astype(np.float32)
        air = np.convolve(air, np.ones(9) / 9, mode='same').astype(np.float32)
        add(start, air * np.sin(np.pi * t / .65) ** 2, .025, -.4)
        if shot['id'] in (14, 16, 25, 37, 49):
            for beat in np.arange(start + 1.2, start + shot['duration'], 1.4):
                hit = np.sin(2 * np.pi * 115 * t) * np.exp(-t * 28) + air * np.exp(-t * 42) * .8
                add(beat, hit, .075, .2)
        if shot['id'] == 2:
            add(start + 42 / 30, air * np.exp(-t * 8) + .6 * np.sin(2 * np.pi * 42 * t) * np.exp(-t * 9), .22)
        if shot['id'] == 44:
            add(start + 2, air * np.exp(-t * 10), .11, .3)
        if shot['id'] == 34:
            add(start + shot['duration'] * .4 + .4, air * np.exp(-t * 44), .055, -.15)
    fade = np.minimum(1, np.arange(len(music)) / (SR * 2))
    fade *= np.minimum(1, np.arange(len(music))[::-1] / (SR * 3))
    music *= fade[:, None].astype(np.float32)
    write_wave(OUT / 'score.wav', music)


def mix_audio():
    graph = '[1:a]volume=0.55[bed];[bed][0:a]sidechaincompress=threshold=0.03:ratio=4:attack=15:release=250[duck];[0:a][duck]amix=inputs=2:normalize=0,aformat=channel_layouts=stereo'
    with tempfile.TemporaryDirectory(prefix='mix-', dir=OUT) as temporary:
        mixed = Path(temporary) / 'stereo.wav'
        command(['ffmpeg', '-v', 'error', '-y', '-threads', '2', '-i', str(OUT / 'narration.wav'),
                 '-i', str(OUT / 'score.wav'), '-filter_complex', graph,
                 '-ar', '48000', '-ac', '2', str(mixed)])
        analysis = command(['ffmpeg', '-v', 'info', '-threads', '2', '-i', str(mixed),
                            '-af', 'loudnorm=I=-16:TP=-1.5:LRA=9:print_format=json', '-f', 'null', '-'])
        measured, _ = json.JSONDecoder().raw_decode(analysis.stderr[analysis.stderr.rfind('{'):])
        normalization = ('loudnorm=I=-16:TP=-1.5:LRA=9:linear=true'
                         f':measured_I={measured["input_i"]}:measured_TP={measured["input_tp"]}'
                         f':measured_LRA={measured["input_lra"]}:measured_thresh={measured["input_thresh"]}'
                         f':offset={measured["target_offset"]}')
        command(['ffmpeg', '-v', 'error', '-y', '-threads', '2', '-i', str(mixed),
                 '-af', normalization, '-ar', '48000', '-ac', '2', str(OUT / 'mix.wav')])


def subtitles_and_chapters(shots):
    cues = json.loads((ROOT / 'src/promo/captions.json').read_text())
    def timestamp(ms):
        h, rem = divmod(ms, 3600000); m, rem = divmod(rem, 60000); s, rem = divmod(rem, 1000)
        return f'{h:02d}:{m:02d}:{s:02d},{rem:03d}'
    (OUT / 'subtitles.srt').write_text('\n'.join(f'{i}\n{timestamp(c["startMs"])} --> {timestamp(c["endMs"])}\n{c["text"]}\n'
                                              for i, c in enumerate(cues, 1)))
    chapter_lines, last = [], None
    for shot in shots:
        if shot['chapter'] != last:
            t = shot['start']; chapter_lines.append(f'{int(t // 60):02d}:{t % 60:04.1f} {shot["chapter"]}')
            last = shot['chapter']
    (OUT / 'chapters.txt').write_text('\n'.join(chapter_lines) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--voice', default='zh-CN-YunxiNeural')
    args = parser.parse_args()
    shots = json.loads(SHOTS.read_text())
    clips = asyncio.run(narrate(shots, args.voice))
    fit_timeline(shots, clips)
    assemble_narration(shots, clips, args.voice)
    del clips
    score(shots)
    mix_audio()
    subtitles_and_chapters(shots)
    print('Completed natural-rate neural narration, measured captions and stereo mix; timeline = 360s.', flush=True)


if __name__ == '__main__':
    main()
