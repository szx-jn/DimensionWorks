"""生成原创合成环境声；只生成 Remotion 音频素材，不编译或打包整合包。

固定种子，90 秒 / 22,050 Hz / 单声道 PCM16。无配音、无外部录音。
"""
from array import array
from math import exp, pi, sin, tanh
from pathlib import Path
import random
import sys
import wave

RATE = 22050
DURATION = 90
DESTINATION = Path(__file__).resolve().parents[1] / "public/audio/intro-soundscape.wav"


def envelope(t, start, end, fade=0.5):
    if t < start or t >= end:
        return 0.0
    return min(1.0, (t - start) / fade, (end - t) / fade)


def heartbeat(t, start, end, strength):
    if not start <= t < end:
        return 0.0
    phase = (t - start) % 1.0
    beat = 0.0
    for offset, gain in ((0, 1), (0.17, 0.55)):
        p = phase - offset
        if 0 <= p < 0.22:
            beat += sin(2 * pi * (64 * p - 30 * p * p)) * exp(-p * 22) * gain
    return beat * strength * envelope(t, start, end)


def generate():
    rng = random.Random(3079)
    samples = array("h")
    wind = 0.0
    peak = 0
    for n in range(RATE * DURATION):
        t = n / RATE
        noise = rng.uniform(-1, 1)
        wind = wind * 0.985 + noise * 0.015
        nature = (envelope(t, 0, 10) + envelope(t, 27, 48)
                  + envelope(t, 79, 90))
        value = wind * 0.35 * nature
        # Soft stream and sparse birds, confined to ordinary nature scenes.
        value += noise * 0.012 * nature * (0.7 + 0.3 * sin(t * 1.7))
        bird_phase = t % 6.1
        if bird_phase < 0.23:
            value += sin(2 * pi * (1700 * bird_phase + 1200 * bird_phase**2)) * sin(pi * bird_phase / 0.23)**2 * 0.025 * nature
        value += heartbeat(t, 10, 21, 0.19)
        value += heartbeat(t, 35, 48, 0.12)
        value += heartbeat(t, 79, 83, 0.12)
        value += heartbeat(t, 87, 88, 0.17)
        # Dimensional disturbance, falling gear and a single restrained impact.
        value += sin(2 * pi * 43 * t) * envelope(t, 2, 10, 2) * 0.025
        fall = t - 22
        if 0 <= fall < 2.6:
            value += noise * (fall / 2.6)**2 * 0.055
        impact = t - 24.6
        if 0 <= impact < 1:
            value += (sin(2 * pi * 72 * impact) * 0.26 + noise * 0.12) * exp(-impact * 10)
        # Powered machine hum: one connected system, not a soundtrack beat.
        machinery = envelope(t, 48, 61, 0.8)
        value += (sin(2 * pi * 88 * t) + 0.3 * sin(2 * pi * 176 * t)) * 0.024 * machinery
        tick = (t - 48) % 0.36
        if tick < 0.025:
            value += noise * exp(-tick * 150) * 0.035 * machinery
        heat = envelope(t, 61, 79, 0.7)
        value += noise * 0.025 * heat
        # Crystal resonance shares a timbre with the return to the heart, not a stated cause.
        for hit in (66.2, 67.8, 69.4, 71.5, 73.8, 76.1, 77.7, 79.1):
            p = t - hit
            if 0 <= p < 0.65:
                value += (sin(2 * pi * 1560 * p) + 0.4 * sin(2 * pi * 2340 * p)) * exp(-p * 9) * 0.035
        value *= envelope(t, 0, 90, 0.7)
        sample = round(tanh(value) * 32767)
        peak = max(peak, abs(sample))
        samples.append(sample)
    if sys.byteorder != "little":
        samples.byteswap()
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(DESTINATION), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(samples.tobytes())
    print(f"原创音景已生成：90 秒，PCM16，峰值 {peak}/32767，无削波。")


if __name__ == "__main__":
    generate()
