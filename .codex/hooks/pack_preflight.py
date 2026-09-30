#!/usr/bin/env python3
"""Codex PreToolUse hook for DimensionWorks packaging.

Only commands that start `scripts/package_mrpack.py` trigger the preflight.
The check is intentionally run with `--skip-build`: `package_mrpack.py` runs
the complete hook, including all in-repo Gradle builds, immediately afterwards.

The hook returns no output for unrelated Bash calls. A successful packaging
preflight returns one additional-context line. A failed preflight denies the
tool call and returns the truncated error log as the denial reason.
"""

from __future__ import annotations

import json
import re
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK_HOOK = ROOT / "scripts" / "pack_hook.py"
PACK_SCRIPT_NAME = "package_mrpack.py"
HOOK_TIMEOUT_SECONDS = 180
MAX_REASON_CHARS = 16000
SHELL_SPLIT = re.compile(r"\s*(?:&&|\|\||[;|\n])\s*")
ASSIGNMENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=.*$")
SHELL_NAMES = {"bash", "dash", "sh", "zsh"}


def executable_and_arguments(tokens: list[str]) -> tuple[str, list[str]]:
    remaining = list(tokens)
    while remaining:
        executable = Path(remaining.pop(0)).name
        if ASSIGNMENT.match(executable):
            continue
        if executable in {"env", "exec", "command", "nohup"}:
            while remaining and remaining[0].startswith("-"):
                remaining.pop(0)
            continue
        if executable in {"nice", "sudo", "timeout"}:
            while remaining and remaining[0].startswith("-"):
                remaining.pop(0)
            if executable in {"nice", "timeout"} and remaining:
                remaining.pop(0)
            continue
        return executable, remaining
    return "", []


def is_packaging_tokens(tokens: list[str], depth: int = 0) -> bool:
    executable, arguments = executable_and_arguments(tokens)
    if not executable:
        return False

    if executable == PACK_SCRIPT_NAME:
        return True

    if executable.startswith("python") and arguments:
        for argument in arguments:
            if Path(argument).name == PACK_SCRIPT_NAME:
                return True
            if argument == "-m" and PACK_SCRIPT_NAME[:-3] in arguments:
                return True

    if depth < 2 and executable in SHELL_NAMES:
        for index, argument in enumerate(arguments):
            if argument.startswith("-") and "c" in argument[1:]:
                if index + 1 < len(arguments):
                    return is_packaging_command(arguments[index + 1], depth + 1)
    return False


def is_packaging_command(command: str, depth: int = 0) -> bool:
    if depth > 2:
        return False
    for segment in SHELL_SPLIT.split(command):
        try:
            tokens = shlex.split(segment, posix=True)
        except ValueError:
            tokens = segment.split()
        if is_packaging_tokens(tokens, depth):
            return True
    return False


def packaging_tokens(command: str) -> list[str]:
    for segment in SHELL_SPLIT.split(command):
        try:
            tokens = shlex.split(segment, posix=True)
        except ValueError:
            tokens = segment.split()
        executable, arguments = executable_and_arguments(tokens)
        if executable == PACK_SCRIPT_NAME:
            return arguments
        if executable.startswith("python"):
            for index, argument in enumerate(arguments):
                if Path(argument).name == PACK_SCRIPT_NAME:
                    return arguments[index + 1 :]
    return []


def option_value(tokens: list[str], option: str) -> str | None:
    for index, token in enumerate(tokens):
        if token == option and index + 1 < len(tokens):
            return tokens[index + 1]
        prefix = f"{option}="
        if token.startswith(prefix):
            return token[len(prefix):]
    return None


def deny(reason: str, system_message: str) -> int:
    reason = reason[:MAX_REASON_CHARS]
    payload = {
        "systemMessage": system_message,
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        },
    }
    json.dump(payload, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")
    return 0


def success(message: str) -> int:
    payload = {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "additionalContext": message,
        }
    }
    json.dump(payload, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")
    return 0


def failure_reason(step: str, detail: str, log: str = "") -> str:
    lines = ["PACK-HOOK FAIL", f"step: {step}", f"detail: {detail}"]
    if log.strip():
        lines.extend(log.strip().splitlines()[-40:])
    return "\n".join(lines)


def main() -> int:
    try:
        event = json.load(sys.stdin)
    except (json.JSONDecodeError, OSError) as error:
        print(
            failure_reason("codex-hook-input", f"cannot read PreToolUse JSON: {error}"),
            file=sys.stderr,
        )
        return 2

    if event.get("hook_event_name") != "PreToolUse" or event.get("tool_name") != "Bash":
        return 0

    tool_input = event.get("tool_input")
    command = tool_input.get("command") if isinstance(tool_input, dict) else None
    if not isinstance(command, str) or not is_packaging_command(command):
        return 0

    tokens = packaging_tokens(command)
    if "--skip-hook" in tokens:
        return deny(
            failure_reason(
                "codex-policy",
                "package_mrpack.py --skip-hook is blocked for Codex packaging; remove it and retry.",
            ),
            "DimensionWorks packaging preflight blocked the call.",
        )

    if not PACK_HOOK.is_file():
        return deny(
            failure_reason("codex-hook", f"missing {PACK_HOOK}"),
            "DimensionWorks packaging preflight could not start.",
        )

    hook_command = [sys.executable, str(PACK_HOOK), "--skip-build"]
    version = option_value(tokens, "--version")
    if version:
        hook_command.extend(["--version", version])
    if "--dimension-complete" in tokens:
        hook_command.append("--dimension-complete")
    if "--major-bump" in tokens:
        hook_command.append("--major-bump")
    if "--instance-dir" in tokens:
        index = tokens.index("--instance-dir")
        if index + 1 < len(tokens):
            hook_command.extend(["--instance-dir", tokens[index + 1]])

    try:
        process = subprocess.run(
            hook_command,
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=HOOK_TIMEOUT_SECONDS,
        )
    except subprocess.TimeoutExpired as error:
        output = (error.stdout or "") if isinstance(error.stdout, str) else ""
        return deny(
            failure_reason(
                "codex-hook-timeout",
                f"pack_hook.py exceeded {HOOK_TIMEOUT_SECONDS}s",
                output,
            ),
            "DimensionWorks packaging preflight timed out.",
        )
    except OSError as error:
        return deny(
            failure_reason("codex-hook", f"cannot run pack_hook.py: {error}"),
            "DimensionWorks packaging preflight could not start.",
        )

    output = "\n".join(part for part in (process.stdout, process.stderr) if part).strip()
    if process.returncode != 0:
        return deny(
            output or failure_reason("pack-hook", f"pack_hook.py exited {process.returncode}"),
            "DimensionWorks packaging preflight failed.",
        )

    summary = output.splitlines()[-1] if output else "PACK-HOOK OK"
    return success(f"Codex packaging preflight: {summary}")


if __name__ == "__main__":
    raise SystemExit(main())
