#!/usr/bin/env python3
"""Generate the DimensionWorks narration with Xiaomi MiMo TTS.

Set MIMO_API_KEY in the environment before running. The key is never written to
disk or printed. MIMO_BASE_URL can override the Token Plan endpoint.
"""

from __future__ import annotations

import base64
import json
import os
import time
import urllib.error
import urllib.request
import wave
from io import BytesIO
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN_PATH = ROOT / "voiceover.json"
OUTPUT_DIR = ROOT / "public" / "audio" / "voiceover"
DEFAULT_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1"


def duration_seconds(audio: bytes) -> float:
    with wave.open(BytesIO(audio), "rb") as wav:
        return wav.getnframes() / wav.getframerate()


def synthesize(api_key: str, base_url: str, model: str, voice: str, style: str, text: str) -> bytes:
    payload = {
        "model": model,
        "messages": [
            {"role": "user", "content": style},
            {"role": "assistant", "content": text},
        ],
        "audio": {"format": "wav", "voice": voice},
    }
    request = urllib.request.Request(
        f"{base_url.rstrip('/')}/chat/completions",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            result = json.loads(response.read())
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", "replace").replace(api_key, "[REDACTED]")[:1200]
        raise RuntimeError(f"MiMo TTS returned HTTP {error.code}: {detail}") from None

    choices = result.get("choices") or []
    message = choices[0].get("message", {}) if choices else {}
    encoded_audio = (message.get("audio") or {}).get("data")
    if not encoded_audio:
        raise RuntimeError("MiMo TTS response did not contain audio data")
    return base64.b64decode(encoded_audio, validate=True)


def main() -> None:
    api_key = os.environ.get("MIMO_API_KEY")
    if not api_key:
        raise SystemExit("Set MIMO_API_KEY in the environment; the key is not stored in this project.")

    base_url = os.environ.get("MIMO_BASE_URL", DEFAULT_BASE_URL)
    plan = json.loads(PLAN_PATH.read_text(encoding="utf-8"))
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    manifest = []
    for index, segment in enumerate(plan["segments"], start=1):
        audio = synthesize(
            api_key,
            base_url,
            plan["model"],
            plan["voice"],
            plan["style"],
            segment["text"],
        )
        duration = duration_seconds(audio)
        filename = f"voiceover-{index:02d}.wav"
        (OUTPUT_DIR / filename).write_bytes(audio)
        manifest.append({**segment, "file": filename, "duration": round(duration, 3)})
        print(f"{filename}: {duration:.2f}s (slot {segment['end'] - segment['start']:.2f}s)")
        time.sleep(0.15)

    (OUTPUT_DIR / "manifest.json").write_text(
        json.dumps({"model": plan["model"], "voice": plan["voice"], "segments": manifest}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    too_long = [item for item in manifest if item["duration"] > item["end"] - item["start"]]
    if too_long:
        raise SystemExit(f"{len(too_long)} voiceover clip(s) exceed their scene slots; adjust narration before preview.")


if __name__ == "__main__":
    main()
