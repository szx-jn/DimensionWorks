#!/usr/bin/env python3
"""Extract requested frames from the actual MP4 and decode all video/audio once."""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def extractFrames(video, frames, output):
    output.mkdir(parents=True, exist_ok=True)
    frames = sorted(set(frames))
    select = 'select=' + '+'.join(f'eq(n\\,{frame})' for frame in frames)
    with tempfile.TemporaryDirectory(prefix='decode-', dir=output) as temporary:
        pattern = str(Path(temporary) / '%03d.png')
        # The final video frame is selected, so both streams are fully decoded.
        subprocess.run(['ffmpeg', '-v', 'error', '-xerror', '-threads', '2', '-i', str(video),
                        '-map', '0:v:0', '-vf', select, '-fps_mode', 'vfr', '-threads', '2',
                        '-start_number', '0', '-y', pattern,
                        '-map', '0:a:0', '-f', 'null', '-'], check=True)
        generated = sorted(Path(temporary).glob('*.png'))
        if len(generated) != len(frames):
            raise RuntimeError(f'Expected {len(frames)} extracted frames, got {len(generated)}')
        for frame, file in zip(frames, generated):
            file.replace(output / f'frame-{frame:05d}.png')
    return frames


def contact_sheet(output, items, name, columns=4):
    tile_w, tile_h = 480, 300
    canvas = Image.new('RGB', (columns * tile_w, ((len(items) + columns - 1) // columns) * tile_h), '#132428')
    draw = ImageDraw.Draw(canvas)
    for i, (frame, label) in enumerate(items):
        image = Image.open(output / f'frame-{frame:05d}.png').convert('RGB')
        image.thumbnail((480, 270))
        x, y = i % columns * tile_w, i // columns * tile_h
        canvas.paste(image, (x, y + 24))
        draw.text((x + 12, y + 6), label, fill='#e6ba84')
    canvas.save(output / name, quality=95)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('video', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'out/qa/extracted')
    args = parser.parse_args()
    metadata = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams',
                                                  '-show_format', '-of', 'json', str(args.video)]))
    video = next(s for s in metadata['streams'] if s['codec_type'] == 'video')
    audio = next(s for s in metadata['streams'] if s['codec_type'] == 'audio')
    assert (video['width'], video['height']) == (1920, 1080)
    assert video['avg_frame_rate'] == '30/1'
    assert int(video['nb_frames']) == 10800
    assert abs(float(metadata['format']['duration']) - 360) < .05
    assert video['codec_name'] == 'h264' and audio['codec_name'] == 'aac'
    assert int(audio['sample_rate']) == 48000 and audio['channels'] == 2
    shots = json.loads((ROOT / 'src/promo/shots.json').read_text())
    requested = [1, 15, 30, 45, 60]
    representatives = [(round(s['start'] * 30 + s['duration'] * 30 * (.8 if s['id'] == 22 else .65)),
                        f'SHOT {s["id"]:02d}') for s in shots]
    frames = extractFrames(args.video, requested + [f for f, _ in representatives] + [10799], args.output)
    contact_sheet(args.output, [(f, f'FRAME {f}') for f in requested], 'required-frames.jpg', columns=3)
    for batch in range(3):
        contact_sheet(args.output, representatives[batch * 20:(batch + 1) * 20], f'scenes-{batch + 1}.jpg')
    loudness = subprocess.run(['ffmpeg', '-v', 'info', '-threads', '2', '-i', str(args.video),
                               '-vn', '-af', 'loudnorm=I=-16:TP=-1.5:LRA=9:print_format=json',
                               '-f', 'null', '-'], check=True, capture_output=True, text=True)
    measurement, _ = json.JSONDecoder().raw_decode(loudness.stderr[loudness.stderr.rfind('{'):])
    assert abs(float(measurement['input_i']) + 16) <= 1, measurement
    assert float(measurement['input_tp']) <= 0, measurement
    report = {'video': str(args.video.resolve()), 'metadata': metadata, 'requiredFrames': requested,
              'extractedFrames': frames, 'fullVideoAudioDecode': 'passed', 'loudness': measurement,
              'result': 'passed'}
    (args.output / 'media-audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(f'Passed: 360s / 10800 frames / 1080p30; full video and audio decoded; extractFrames({requested}); '
          f'{measurement["input_i"]} LUFS, {measurement["input_tp"]} dBTP.')


if __name__ == '__main__':
    main()
