#!/usr/bin/env python3
"""Packaging preflight hook for DimensionWorks.

Runs every check that must pass before `scripts/package_mrpack.py` builds a
`.mrpack`, and reports only two things:

* on failure: the failing step name plus the captured error log
* on success: a single summary line

`package_mrpack.py` calls this automatically, so a normal packaging run cannot
skip the hook unless it is asked to with `--skip-hook`.

Usage:
    python3 scripts/pack_hook.py            # terse: errors or one success line
    python3 scripts/pack_hook.py --verbose  # also print each step result
    python3 scripts/pack_hook.py --skip-build
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "manifest" / "mods.json"
DEFAULT_INSTANCE_DIR = (
    Path.home() / "Library" / "Application Support" / "minecraft" / "versions" / "DimensionWorks"
)
ERROR_LOG_LINES = 40
BUILD_TIMEOUT_SECONDS = 900

KUBEJS_JSON_ROOTS = ("kubejs/data", "kubejs/assets")
SELF_PROPAGATION_CHECK = ROOT / "scripts" / "check_cave_self_propagation.js"
QUEST_CHAPTER_DIR = ROOT / "config" / "ftbquests" / "quests" / "chapters"
RECIPE_SOURCE_DIRS = ("kubejs/server_scripts", "kubejs/startup_scripts", "kubejs/client_scripts")


@dataclass
class CheckResult:
    name: str
    ok: bool
    detail: str = ""
    log: list[str] = field(default_factory=list)


def run(command: list[str], cwd: Path) -> tuple[int, list[str]]:
    process = subprocess.run(
        command,
        cwd=str(cwd),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        timeout=BUILD_TIMEOUT_SECONDS,
    )
    return process.returncode, process.stdout.splitlines()


def tail(lines: list[str]) -> list[str]:
    if len(lines) <= ERROR_LOG_LINES:
        return lines
    return [f"... {len(lines) - ERROR_LOG_LINES} earlier lines omitted ...", *lines[-ERROR_LOG_LINES:]]


def check_manifest() -> CheckResult:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    problems: list[str] = []
    required_fields = {
        "modrinth": ("project_id", "version_id"),
        "curseforge": ("file_id", "file_name"),
        "github-release": ("repository", "release", "asset"),
        "in-repo": ("project_path", "version"),
    }
    for mod in manifest.get("mods", []):
        source = mod.get("source")
        if source not in required_fields:
            problems.append(f"{mod.get('id')}: unsupported source {source!r}")
            continue
        for field_name in required_fields[source]:
            value = mod.get(field_name)
            if not value:
                problems.append(f"{mod.get('id')}: missing {field_name}")
            elif isinstance(value, str) and "pending" in value.lower():
                problems.append(f"{mod.get('id')}: {field_name}={value} is not a fixed version")
    return CheckResult("manifest", not problems, f"{len(manifest.get('mods', []))} mods", problems)


def check_instance(instance_dir: Path) -> CheckResult:
    missing = [str(path) for path in (instance_dir / "config",) if not path.is_dir()]
    return CheckResult("instance-config", not missing, str(instance_dir), missing)


def check_json() -> CheckResult:
    problems: list[str] = []
    files = [MANIFEST]
    for relative in KUBEJS_JSON_ROOTS:
        files.extend(sorted((ROOT / relative).rglob("*.json")))
    quest_root = ROOT / "config" / "ftbquests"
    if quest_root.is_dir():
        files.extend(sorted(quest_root.rglob("*.json")))
    for path in files:
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
            problems.append(f"{path.relative_to(ROOT)}: {error}")
    return CheckResult("json", not problems, f"{len(files)} files", problems)


def check_kubejs_syntax() -> CheckResult:
    node = shutil.which("node")
    files = sorted(
        path for directory in RECIPE_SOURCE_DIRS for path in (ROOT / directory).glob("*.js")
    )
    if not files:
        return CheckResult("kubejs-syntax", True, "no scripts")
    if node is None:
        return CheckResult("kubejs-syntax", True, f"skipped, node missing ({len(files)} scripts)")
    problems: list[str] = []
    for path in files:
        code, output = run([node, "--check", str(path)], ROOT)
        if code != 0:
            problems.append(f"{path.relative_to(ROOT)}: {' / '.join(output[-3:])}")
    return CheckResult("kubejs-syntax", not problems, f"{len(files)} scripts", problems)


def check_self_propagation() -> CheckResult:
    """Re-run the cave self-propagation recipe rules against a stub Create API."""
    node = shutil.which("node")
    if node is None:
        return CheckResult("self-propagation", True, "skipped, node missing")
    if not SELF_PROPAGATION_CHECK.is_file():
        return CheckResult("self-propagation", True, "checker not present")
    code, output = run([node, str(SELF_PROPAGATION_CHECK)], ROOT)
    detail = output[-1] if output else f"exit {code}"
    return CheckResult("self-propagation", code == 0, detail, output if code else [])


def check_quest_graph() -> CheckResult:
    if not QUEST_CHAPTER_DIR.is_dir():
        return CheckResult("quest-graph", True, "no quests")
    problems: list[str] = []
    chapters = sorted(QUEST_CHAPTER_DIR.glob("*.snbt"))
    quests: dict[str, set[str]] = {}
    seen: dict[str, str] = {}

    for path in chapters:
        text = path.read_text(encoding="utf-8")
        depth = 0
        for char in text:
            if char in "{[":
                depth += 1
            elif char in "}]":
                depth -= 1
            if depth < 0:
                problems.append(f"{path.name}: unbalanced brackets")
                break
        if depth != 0:
            problems.append(f"{path.name}: unbalanced brackets (depth {depth})")
        for raw_id in re.findall(r'^\s+id: "([0-9A-Fa-f]+)"', text, re.MULTILINE):
            origin = seen.get(raw_id)
            if origin:
                problems.append(f"{path.name}: duplicate quest/task id {raw_id} (also in {origin})")
            else:
                seen[raw_id] = path.name
        for block in text.split("\n\t\t{")[1:]:
            id_match = re.search(r'^\s*id: "([0-9A-Fa-f]+)"', block, re.MULTILINE)
            if not id_match:
                continue
            dependency_match = re.search(r"dependencies: \[([^\]]*)\]", block)
            dependencies = (
                set(re.findall(r'"([0-9A-Fa-f]+)"', dependency_match.group(1)))
                if dependency_match
                else set()
            )
            quests[id_match.group(1)] = dependencies

    for quest_id, dependencies in quests.items():
        for dependency in dependencies:
            if dependency not in quests:
                problems.append(f"{quest_id}: missing dependency {dependency}")

    colour: dict[str, int] = {}
    for start in quests:
        stack = [start]
        while stack:
            current = stack.pop()
            state = colour.get(current, 0)
            if state == 2:
                continue
            if state == 1:
                problems.append(f"quest dependency cycle through {current}")
                break
            colour[current] = 1
            stack.extend(quests.get(current, ()))
            colour[current] = 2

    return CheckResult(
        "quest-graph", not problems, f"{len(chapters)} chapters, {len(quests)} quests", problems
    )


def datapack_recipe_ids() -> dict[str, str]:
    identifiers: dict[str, str] = {}
    for path in (ROOT / "kubejs" / "data").rglob("recipes/**/*.json"):
        relative = path.relative_to(ROOT / "kubejs" / "data")
        namespace = relative.parts[0]
        rest = Path(*relative.parts[2:]).with_suffix("")
        identifiers.setdefault(f"{namespace}:{rest.as_posix()}", str(path.relative_to(ROOT)))
    return identifiers


def check_recipe_ids() -> CheckResult:
    problems: list[str] = []
    identifiers = datapack_recipe_ids()
    pattern = re.compile(r"\.id\(\s*(['\"])([^'\"]+)\1\s*\)")
    count = 0
    for directory in RECIPE_SOURCE_DIRS:
        base = ROOT / directory
        if not base.is_dir():
            continue
        for path in sorted(base.glob("*.js")):
            for _, identifier in pattern.findall(path.read_text(encoding="utf-8")):
                count += 1
                origin = identifiers.get(identifier)
                if origin:
                    problems.append(
                        f"{path.relative_to(ROOT)}: {identifier} collides with {origin}"
                    )
                else:
                    identifiers[identifier] = str(path.relative_to(ROOT))
    return CheckResult(
        "recipe-ids", not problems, f"{count} scripted ids, {len(identifiers)} unique", problems
    )


def in_repo_mods() -> list[dict[str, object]]:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    return [mod for mod in manifest.get("mods", []) if mod.get("source") == "in-repo"]


def check_builds() -> CheckResult:
    mods = in_repo_mods()
    if not mods:
        return CheckResult("in-repo-build", True, "no in-repo mods")
    if shutil.which("gradle") is None:
        return CheckResult("in-repo-build", False, "gradle not found on PATH", [])
    log: list[str] = []
    built: list[str] = []
    for mod in mods:
        project = ROOT / str(mod["project_path"])
        code, output = run(["gradle", "build", "--no-daemon"], project)
        log.extend([f"$ gradle build --no-daemon  ({project.relative_to(ROOT)})", *output])
        jar = project / "build" / "libs" / f"{mod['id']}-{mod['version']}.jar"
        if code != 0:
            return CheckResult("in-repo-build", False, f"{mod['id']}: gradle exited {code}", log)
        if not jar.is_file():
            log.append(f"missing {jar}")
            return CheckResult("in-repo-build", False, f"{mod['id']}: jar not produced", log)
        built.append(jar.name)
    return CheckResult("in-repo-build", True, ", ".join(built), log)


def run_all(instance_dir: Path, *, skip_build: bool, verbose: bool) -> tuple[bool, str]:
    started = time.monotonic()
    checks = [
        check_manifest,
        lambda: check_instance(instance_dir),
        check_json,
        check_kubejs_syntax,
        check_self_propagation,
        check_quest_graph,
        check_recipe_ids,
    ]
    results: list[CheckResult] = []
    for check in checks:
        result = check()
        results.append(result)
        if verbose:
            print(f"{'ok  ' if result.ok else 'FAIL'} {result.name}: {result.detail}")
        if not result.ok:
            return False, format_failure(result)

    if skip_build:
        if verbose:
            print("skip in-repo-build: requested by --skip-build")
    else:
        build_result = check_builds()
        results.append(build_result)
        if verbose:
            print(f"{'ok  ' if build_result.ok else 'FAIL'} {build_result.name}: {build_result.detail}")
        if not build_result.ok:
            return False, format_failure(build_result)

    elapsed = time.monotonic() - started
    build_summary = f"{len(in_repo_mods())} in-repo mods built" if not skip_build else "in-repo builds skipped"
    summary = f"{len(results)} checks, {build_summary}, {elapsed:.1f}s"
    return True, summary


def format_failure(result: CheckResult) -> str:
    lines = [f"step: {result.name}", f"detail: {result.detail or 'failed'}"]
    if result.log:
        lines.extend(tail(result.log))
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verbose", action="store_true", help="print every step result")
    parser.add_argument("--skip-build", action="store_true", help="skip in-repo mod builds")
    parser.add_argument(
        "--instance-dir",
        type=Path,
        default=DEFAULT_INSTANCE_DIR,
        help="game instance root containing config/ (default: %(default)s)",
    )
    args = parser.parse_args()

    ok, message = run_all(
        args.instance_dir.expanduser().resolve(), skip_build=args.skip_build, verbose=args.verbose
    )
    if not ok:
        print("PACK-HOOK FAIL")
        print(message)
        return 1
    print(f"PACK-HOOK OK: {message}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
