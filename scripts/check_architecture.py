#!/usr/bin/env python3
"""Check self-authored mod dependency cycles and selected layer boundaries."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
IMPORT_RE = re.compile(
    r"^\s*import\s+(?:static\s+)?([\w]+(?:\.[\w]+)*)(?:\.\*)?\s*;",
    re.MULTILINE,
)
PROJECT_DEPENDENCY_RE = re.compile(
    r"\bproject\s*\(\s*(?:path\s*:\s*)?['\"]:([\w-]+)['\"]\s*\)"
)

FORBIDDEN_IMPORTS = {
    "dimensionworks-rpm-limit": (
        (".mekanism.", "generic RPM/quota module imports Mekanism implementation"),
        (".mekstress.", "generic RPM/quota module imports mek-stress implementation"),
    ),
}

FORBIDDEN_PACKAGE_IMPORTS = {
    "dimensionworks-mek-stress": {
        "dev.szx.dimensionworks.cavefactory": "mek-stress may only consume the Cave Factory API",
    },
}

FORBIDDEN_SOURCE_NAMESPACE_IMPORTS = {
    "dimensionworks-rpm-limit": (
        (
            ".api.",
            "dev.szx.dimensionworks.rpmlimit",
            "public RPM API imports implementation packages",
        ),
    ),
}

FORBIDDEN_LAYER_IMPORTS = {
    "dimensionworks-cave-factory": (
        (".logic.", ".registry.", "logic package imports registry package"),
        (".api.", ".blockentity.", "public API imports block-entity implementation"),
        (".api.", ".item.", "public API imports item implementation"),
        (".api.", ".menu.", "public API imports menu implementation"),
        (".api.", ".registry.", "public API imports registry implementation"),
    ),
    "dimensionworks-transfer": (
        (".network.", ".dispatch.", "network package imports dispatch implementation"),
        (".network.", ".tutorial.", "network package imports tutorial implementation"),
        (".network.", ".handler.", "network package imports event-handler implementation"),
        (".network.", ".territory.", "network package imports territory implementation"),
        (".network.", ".elastic.", "network package imports elastic implementation"),
    ),
}


def module_sources(module: Path) -> list[Path]:
    return sorted((module / "src/main/java").rglob("*.java"))


def strongly_connected_components(graph: dict[str, set[str]]) -> list[list[str]]:
    index = 0
    indices: dict[str, int] = {}
    lowlinks: dict[str, int] = {}
    stack: list[str] = []
    on_stack: set[str] = set()
    components: list[list[str]] = []

    def visit(node: str) -> None:
        nonlocal index
        indices[node] = lowlinks[node] = index
        index += 1
        stack.append(node)
        on_stack.add(node)
        for neighbor in graph[node]:
            if neighbor not in indices:
                visit(neighbor)
                lowlinks[node] = min(lowlinks[node], lowlinks[neighbor])
            elif neighbor in on_stack:
                lowlinks[node] = min(lowlinks[node], indices[neighbor])
        if lowlinks[node] == indices[node]:
            component = []
            while True:
                member = stack.pop()
                on_stack.remove(member)
                component.append(member)
                if member == node:
                    break
            if len(component) > 1 or node in graph[node]:
                components.append(sorted(component))

    for node in graph:
        if node not in indices:
            visit(node)
    return sorted(components)


def gradle_project_dependencies(source: str) -> set[str]:
    return set(PROJECT_DEPENDENCY_RE.findall(source))


def inspect_module(
    module: Path,
    module_by_type: dict[str, str],
    module_by_package: dict[str, str],
) -> tuple[set[str], list[str]]:
    module_name = module.name
    package_by_file: dict[Path, str] = {}
    for source in module_sources(module):
        match = PACKAGE_RE.search(source.read_text(encoding="utf-8"))
        if match is not None:
            package_by_file[source] = match.group(1)

    module_edges: set[str] = set()
    violations: list[str] = []
    for source, package in package_by_file.items():
        text = source.read_text(encoding="utf-8")
        for imported in IMPORT_RE.findall(text):
            target_package = imported.rpartition(".")[0] if "." in imported else imported
            owner = module_by_type.get(imported) or module_by_package.get(target_package)
            if owner is not None and owner != module_name:
                module_edges.add(owner)

            for fragment, reason in FORBIDDEN_IMPORTS.get(module_name, ()):
                if fragment in f".{imported}.":
                    violations.append(f"{source.relative_to(ROOT)}: {reason}: {imported}")

            for forbidden_prefix, reason in FORBIDDEN_PACKAGE_IMPORTS.get(module_name, {}).items():
                if imported.startswith(forbidden_prefix + ".") and not imported.startswith(
                    forbidden_prefix + ".api."
                ):
                    violations.append(f"{source.relative_to(ROOT)}: {reason}: {imported}")

            for source_fragment, target_prefix, reason in FORBIDDEN_SOURCE_NAMESPACE_IMPORTS.get(
                module_name, ()
            ):
                if (
                    source_fragment in f".{package}."
                    and imported.startswith(target_prefix + ".")
                    and not imported.startswith(target_prefix + ".api.")
                ):
                    violations.append(f"{source.relative_to(ROOT)}: {reason}: {imported}")

            for source_fragment, target_fragment, reason in FORBIDDEN_LAYER_IMPORTS.get(
                module_name, ()
            ):
                if (
                    source_fragment in f".{package}."
                    and imported.startswith("dev.szx.dimensionworks.")
                    and target_fragment in f".{imported}."
                ):
                    violations.append(f"{source.relative_to(ROOT)}: {reason}: {imported}")

    return module_edges, sorted(set(violations))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module", action="append", help="check only these module directory names")
    arguments = parser.parse_args()
    settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
    include_section = settings.split("include", 1)[1].split("rootProject.children", 1)[0]
    configured_modules = re.findall(r"['\"]([^'\"]+)['\"]", include_section)
    modules = [ROOT / "mods" / name for name in configured_modules]
    missing_paths = [str(path.relative_to(ROOT)) for path in modules if not path.is_dir()]
    if missing_paths:
        print(f"configured module directories missing: {', '.join(missing_paths)}", file=sys.stderr)
        return 2
    selected = set(arguments.module or ())
    if selected:
        modules = [module for module in modules if module.name in selected]
        missing = selected - {module.name for module in modules}
        if missing:
            print(f"unknown module(s): {', '.join(sorted(missing))}", file=sys.stderr)
            return 2

    module_by_type: dict[str, str] = {}
    module_by_package: dict[str, str] = {}
    for module in modules:
        for source in module_sources(module):
            match = PACKAGE_RE.search(source.read_text(encoding="utf-8"))
            if match is None:
                continue
            package = match.group(1)
            module_by_type[f"{package}.{source.stem}"] = module.name
            module_by_package[package] = module.name

    module_graph: dict[str, set[str]] = {module.name: set() for module in modules}
    failures: list[str] = []
    for module in modules:
        module_edges, violations = inspect_module(module, module_by_type, module_by_package)
        failures.extend(violations)
        module_graph[module.name].update(module_edges)
        build_file = module / "build.gradle"
        if build_file.is_file():
            dependencies = gradle_project_dependencies(build_file.read_text(encoding="utf-8"))
            for dependency in sorted(dependencies):
                if dependency not in module_graph:
                    failures.append(
                        f"{build_file.relative_to(ROOT)}: project dependency is not included "
                        f"in settings.gradle: {dependency}"
                    )
                elif dependency != module.name:
                    module_graph[module.name].add(dependency)

    for component in strongly_connected_components(module_graph):
        failures.append(f"module dependency cycle: {' -> '.join(component + [component[0]])}")

    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(
        f"Architecture checks passed for {len(modules)} module(s): "
        "no module cycles or forbidden layer imports."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
