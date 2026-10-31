#!/usr/bin/env python3
"""Validate DimensionWorks localization coverage against installed third-party jars."""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INSTANCE_DIR = (
    Path.home() / "Library" / "Application Support" / "minecraft" / "versions" / "DimensionWorks"
)
DEFAULT_OVERRIDE_ROOT = ROOT / "mods" / "dimensionworks-localization"
LOCALIZATION_JAR_PREFIX = "dimensionworks_localization-"
OPTIONAL_NAMESPACE_FILE = "optional-namespaces.json"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]|%%")
LANG_FILE = re.compile(r"assets/([^/]+)/lang/(en_us|zh_cn)\.json")
GUIDEME_PAGE = re.compile(r"assets/([^/]+)/([^/]+)/(.+\.md)")
PATCHOULI_PAGE = re.compile(
    r"assets/([^/]+)/patchouli_books/([^/]+)/(en_us|zh_cn)/(.+\.json)"
)


@dataclass
class LocalizationReport:
    scanned_jars: int = 0
    language_key_count: int = 0
    manual_page_count: int = 0
    namespace_count: int = 0
    missing_language_keys: list[tuple[str, str]] = field(default_factory=list)
    unknown_override_keys: list[tuple[str, str]] = field(default_factory=list)
    redundant_override_keys: list[tuple[str, str]] = field(default_factory=list)
    duplicate_override_keys: list[tuple[str, str]] = field(default_factory=list)
    placeholder_mismatches: list[tuple[str, str, list[str], list[str]]] = field(
        default_factory=list
    )
    json_errors: list[str] = field(default_factory=list)
    missing_manual_pages: list[tuple[str, str, str, str]] = field(default_factory=list)
    unknown_manual_overrides: list[tuple[str, str, str, str]] = field(default_factory=list)
    redundant_manual_overrides: list[tuple[str, str, str, str]] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not any(
            (
                self.missing_language_keys,
                self.unknown_override_keys,
                self.redundant_override_keys,
                self.duplicate_override_keys,
                self.placeholder_mismatches,
                self.json_errors,
                self.missing_manual_pages,
                self.unknown_manual_overrides,
                self.redundant_manual_overrides,
            )
        )


def _load_json_object(text: str) -> tuple[dict[str, Any], list[str]]:
    duplicates: list[str] = []

    def object_pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                duplicates.append(key)
            result[key] = value
        return result

    value = json.loads(text, object_pairs_hook=object_pairs)
    if not isinstance(value, dict):
        raise ValueError("expected a top-level JSON object")
    return value, duplicates


def _placeholder_signature(value: str) -> list[str]:
    return PLACEHOLDER.findall(value)


def _placeholder_shape(value: str) -> dict[int, list[str]]:
    """Return argument indexes and conversion types, ignoring argument order."""
    shape: dict[int, list[str]] = defaultdict(list)
    anonymous_index = 0
    for token in PLACEHOLDER.findall(value):
        if token == "%%":
            continue
        conversion = token[-1]
        explicit_match = re.fullmatch(r"%(\d+)\$[sdf]", token)
        if explicit_match is not None:
            argument_index = int(explicit_match.group(1))
        else:
            anonymous_index += 1
            argument_index = anonymous_index
        shape[argument_index].append(conversion)
    return dict(shape)


def _read_lang_files(
    archive: zipfile.ZipFile,
) -> tuple[dict[str, dict[str, str]], dict[str, dict[str, str]], list[str]]:
    source_en: dict[str, dict[str, str]] = {}
    source_zh: dict[str, dict[str, str]] = {}
    errors: list[str] = []
    for name in archive.namelist():
        match = LANG_FILE.fullmatch(name)
        if match is None:
            continue
        namespace, language = match.groups()
        try:
            values, _ = _load_json_object(archive.read(name).decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as error:
            errors.append(f"{archive.filename}:{name}: {error}")
            continue
        target = source_en if language == "en_us" else source_zh
        target.setdefault(namespace, {}).update(
            {key: value for key, value in values.items() if isinstance(value, str)}
        )
    return source_en, source_zh, errors


def _read_override_lang_files(
    override_root: Path,
) -> tuple[dict[str, dict[str, str]], list[tuple[str, str]], list[str]]:
    overrides: dict[str, dict[str, str]] = {}
    duplicates: list[tuple[str, str]] = []
    errors: list[str] = []
    assets = override_root / "assets"
    if not assets.is_dir():
        return overrides, duplicates, errors
    for path in sorted(assets.glob("*/lang/zh_cn.json")):
        namespace = path.parents[1].name
        try:
            values, duplicate_keys = _load_json_object(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError, ValueError) as error:
            errors.append(f"{path.relative_to(override_root)}: {error}")
            continue
        duplicates.extend((namespace, key) for key in duplicate_keys)
        overrides.setdefault(namespace, {}).update(
            {key: value for key, value in values.items() if isinstance(value, str)}
        )
    return overrides, duplicates, errors


def _read_optional_namespaces(override_root: Path) -> tuple[set[str], list[str]]:
    path = override_root / OPTIONAL_NAMESPACE_FILE
    if not path.exists():
        return set(), []
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        return set(), [f"{path.relative_to(override_root)}: {error}"]
    if not isinstance(value, list) or any(
        not isinstance(namespace, str) or not namespace for namespace in value
    ):
        return set(), [f"{path.relative_to(override_root)}: expected a list of namespace strings"]
    if len(value) != len(set(value)):
        return set(), [f"{path.relative_to(override_root)}: duplicate namespace"]
    return set(value), []


def _guideme_sets(names: list[str]) -> dict[tuple[str, str], dict[str, set[str]]]:
    guides: dict[tuple[str, str], dict[str, set[str]]] = defaultdict(
        lambda: defaultdict(set)
    )
    for name in names:
        match = GUIDEME_PAGE.fullmatch(name)
        if match is None:
            continue
        namespace, guide, relative = match.groups()
        if not guide.endswith("guide"):
            continue
        parts = relative.split("/")
        language = "default"
        if parts and re.fullmatch(r"_[a-z]{2}_[a-z]{2}", parts[0]):
            language = parts.pop(0)
        relative = "/".join(parts)
        guides[(namespace, guide)][language].add(relative)
    return guides


def _patchouli_sets(names: list[str]) -> dict[tuple[str, str], dict[str, set[str]]]:
    books: dict[tuple[str, str], dict[str, set[str]]] = defaultdict(
        lambda: defaultdict(set)
    )
    for name in names:
        match = PATCHOULI_PAGE.fullmatch(name)
        if match is None:
            continue
        namespace, book, language, relative = match.groups()
        books[(namespace, book)][language].add(relative)
    return books


def _override_files(
    override_root: Path,
    namespace: str,
    root_name: str,
    language: str,
    extension: str,
) -> set[str]:
    base = override_root / "assets" / namespace / root_name / language
    if not base.is_dir():
        return set()
    return {
        path.relative_to(base).as_posix()
        for path in base.rglob(f"*{extension}")
        if path.is_file()
    }


def build_report(instance_dir: Path, override_root: Path) -> LocalizationReport:
    report = LocalizationReport()
    source_en: dict[str, dict[str, str]] = defaultdict(dict)
    source_zh: dict[str, dict[str, str]] = defaultdict(dict)
    guideme: dict[tuple[str, str], dict[str, set[str]]] = defaultdict(
        lambda: defaultdict(set)
    )
    patchouli: dict[tuple[str, str], dict[str, set[str]]] = defaultdict(
        lambda: defaultdict(set)
    )

    mods_dir = instance_dir / "mods"
    for jar in sorted(mods_dir.glob("*.jar")):
        if jar.name.startswith(LOCALIZATION_JAR_PREFIX):
            continue
        try:
            with zipfile.ZipFile(jar) as archive:
                names = archive.namelist()
                en, zh, errors = _read_lang_files(archive)
                report.json_errors.extend(errors)
                for namespace, values in en.items():
                    source_en[namespace].update(values)
                for namespace, values in zh.items():
                    source_zh[namespace].update(values)
                for key, languages in _guideme_sets(names).items():
                    for language, pages in languages.items():
                        guideme[key][language].update(pages)
                for key, languages in _patchouli_sets(names).items():
                    for language, pages in languages.items():
                        patchouli[key][language].update(pages)
                report.scanned_jars += 1
        except (OSError, zipfile.BadZipFile) as error:
            report.json_errors.append(f"{jar}: {error}")

    overrides, duplicate_keys, override_errors = _read_override_lang_files(override_root)
    optional_namespaces, optional_errors = _read_optional_namespaces(override_root)
    report.duplicate_override_keys.extend(duplicate_keys)
    report.json_errors.extend(override_errors)
    report.json_errors.extend(optional_errors)
    report.namespace_count = len(overrides)

    for namespace in sorted(set(source_en) | set(overrides)):
        english = source_en.get(namespace, {})
        existing = source_zh.get(namespace, {})
        override = overrides.get(namespace, {})
        if namespace in optional_namespaces and not english and not existing:
            continue
        for key in sorted(set(override) - set(english)):
            report.unknown_override_keys.append((namespace, key))
        for key in sorted(set(override) & set(existing)):
            report.redundant_override_keys.append((namespace, key))
        for key in sorted(set(english) - set(existing)):
            translated = override.get(key)
            if translated is None:
                report.missing_language_keys.append((namespace, key))
                continue
            report.language_key_count += 1
            expected = _placeholder_signature(english[key])
            actual = _placeholder_signature(translated)
            if _placeholder_shape(english[key]) != _placeholder_shape(translated):
                report.placeholder_mismatches.append(
                    (namespace, key, expected, actual)
                )

    for (namespace, guide), languages in sorted(guideme.items()):
        defaults = languages.get("default", set())
        existing = languages.get("_zh_cn", set()) | languages.get("zh_cn", set())
        override = _override_files(
            override_root,
            namespace,
            guide,
            "_zh_cn",
            ".md",
        )
        for page in sorted(override - defaults - existing):
            report.unknown_manual_overrides.append(
                ("guideme", namespace, guide, page)
            )
        for page in sorted(override & existing):
            report.redundant_manual_overrides.append(
                ("guideme", namespace, guide, page)
            )
        baseline_missing = defaults - existing
        covered = baseline_missing & override
        report.manual_page_count += len(covered)
        for page in sorted(baseline_missing - override):
            report.missing_manual_pages.append(
                ("guideme", namespace, guide, page)
            )

    for (namespace, book), languages in sorted(patchouli.items()):
        defaults = languages.get("en_us", set())
        existing = languages.get("zh_cn", set())
        override = _override_files(
            override_root,
            namespace,
            "patchouli_books",
            f"{book}/zh_cn",
            ".json",
        )
        for page in sorted(override - defaults - existing):
            report.unknown_manual_overrides.append(
                ("patchouli", namespace, book, page)
            )
        for page in sorted(override & existing):
            report.redundant_manual_overrides.append(
                ("patchouli", namespace, book, page)
            )
        baseline_missing = defaults - existing
        covered = baseline_missing & override
        report.manual_page_count += len(covered)
        for page in sorted(baseline_missing - override):
            report.missing_manual_pages.append(
                ("patchouli", namespace, book, page)
            )

    report.missing_language_keys.sort()
    report.unknown_override_keys.sort()
    report.redundant_override_keys.sort()
    report.duplicate_override_keys.sort()
    report.missing_manual_pages.sort()
    report.unknown_manual_overrides.sort()
    report.redundant_manual_overrides.sort()
    return report


def format_failure(report: LocalizationReport, limit: int = 40) -> str:
    groups = [
        ("missing-language-keys", report.missing_language_keys),
        ("unknown-override-keys", report.unknown_override_keys),
        ("redundant-override-keys", report.redundant_override_keys),
        ("duplicate-override-keys", report.duplicate_override_keys),
        ("placeholder-mismatches", report.placeholder_mismatches),
        ("json-errors", report.json_errors),
        ("missing-manual-pages", report.missing_manual_pages),
        ("unknown-manual-overrides", report.unknown_manual_overrides),
        ("redundant-manual-overrides", report.redundant_manual_overrides),
    ]
    lines = ["LOCALIZATION FAIL"]
    for name, values in groups:
        if not values:
            continue
        lines.append(f"{name}: {len(values)}")
        for value in values[:limit]:
            lines.append(f"  {value}")
        if len(values) > limit:
            lines.append(f"  ... {len(values) - limit} more")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--instance-dir",
        type=Path,
        default=DEFAULT_INSTANCE_DIR,
        help="game instance containing mods/ (default: %(default)s)",
    )
    parser.add_argument(
        "--override-root",
        type=Path,
        default=DEFAULT_OVERRIDE_ROOT,
        help="localization mod project root (default: %(default)s)",
    )
    args = parser.parse_args()
    report = build_report(
        args.instance_dir.expanduser().resolve(),
        args.override_root.expanduser().resolve(),
    )
    if not report.ok:
        print(format_failure(report))
        return 1
    print(
        "LOCALIZATION OK: "
        f"{report.scanned_jars} jars, "
        f"{report.namespace_count} namespaces, "
        f"{report.language_key_count} language keys, "
        f"{report.manual_page_count} manual pages"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
