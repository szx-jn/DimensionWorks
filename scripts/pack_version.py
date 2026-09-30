#!/usr/bin/env python3
"""Pack version rules for DimensionWorks releases."""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "VERSION"
VERSION_PATTERN = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


class VersionRuleError(ValueError):
    """Raised when a requested pack version violates the release rules."""


@dataclass(frozen=True, order=True)
class PackVersion:
    major: int
    minor: int
    patch: int

    @classmethod
    def parse(cls, value: str) -> "PackVersion":
        raw = value.strip()
        match = VERSION_PATTERN.fullmatch(raw)
        if match is None:
            raise VersionRuleError(
                f"整合包版本必须使用 x.x.x 三段数字格式，不接受 alpha、beta 或前缀：{value!r}"
            )
        return cls(*(int(part) for part in match.groups()))

    def __str__(self) -> str:
        return f"{self.major}.{self.minor}.{self.patch}"

    def next_patch(self) -> "PackVersion":
        return PackVersion(self.major, self.minor, self.patch + 1)

    def next_minor(self) -> "PackVersion":
        return PackVersion(self.major, self.minor + 1, 0)

    def next_major(self) -> "PackVersion":
        return PackVersion(self.major + 1, 0, 0)


def read_current_version(path: Path = VERSION_FILE) -> PackVersion:
    try:
        raw = path.read_text(encoding="utf-8")
    except OSError as error:
        raise VersionRuleError(f"无法读取版本文件 {path}: {error}") from error
    return PackVersion.parse(raw)


def write_current_version(version: PackVersion, path: Path = VERSION_FILE) -> None:
    path.write_text(f"{version}\n", encoding="utf-8")


def resolve_version(
    requested: str | None,
    *,
    dimension_complete: bool = False,
    major_bump: bool = False,
    current: PackVersion | None = None,
) -> PackVersion:
    baseline = current or read_current_version()
    if dimension_complete and major_bump:
        raise VersionRuleError("同一版本不能同时标记维度完成和主版本提升")

    if major_bump:
        expected = baseline.next_major()
    elif dimension_complete:
        expected = baseline.next_minor()
    else:
        expected = baseline.next_patch()

    if requested is None:
        return expected

    target = PackVersion.parse(requested)
    if target != expected:
        if major_bump:
            rule = f"主版本提升必须写成 {expected}"
        elif dimension_complete:
            rule = f"完成维度后必须把次版本加一并清零补丁号，写成 {expected}"
        else:
            rule = f"普通小更新只能把补丁号加一，写成 {expected}"
        raise VersionRuleError(f"版本必须从 {baseline} 递增到 {expected}；{rule}，实际为 {target}")

    return target
