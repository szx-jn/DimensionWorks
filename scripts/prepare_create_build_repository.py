#!/usr/bin/env python3
"""CI-only: stage pinned Create build dependencies from the verified pack jar.

The upstream Maven certificate has expired. Modrinth's fixed, hashed bundle
contains the exact dependency versions we compile against. No Java class bytes
are modified, and the distributed Create jar remains the original bundle.
"""
from __future__ import annotations

import hashlib
import io
import json
import os
from pathlib import Path
import urllib.request
import zipfile

CREATE_VERSION_ID = "8amzvn9x"
CREATE_SHA512 = "5b51f5d31437d9bfbf221b67459abd87260d284874e1e23b7ed1d522b23a48059a3b1d2c1c541428da35dc6be15561b4f3c7f36aac6df3a0e8db6a278f6b0316"
EXPECTED = {
    ("com.tterrag.registrate", "Registrate"): "MC1.20-1.3.3",
    ("dev.engine-room.flywheel", "flywheel-forge-1.20.1"): "1.0.5",
    ("net.createmod.ponder", "Ponder-Forge-1.20.1"): "1.0.91",
    ("io.github.llamalad7", "mixinextras-forge"): "0.4.1",
}


def stage(repo: Path, group: str, artifact: str, version: str, payload: bytes, classifier: str = "",
          dependencies: dict[tuple[str, str], str] | None = None) -> None:
    directory = repo / group.replace(".", "/") / artifact / version
    directory.mkdir(parents=True, exist_ok=True)
    stem = f"{artifact}-{version}"
    name = stem + (f"-{classifier}" if classifier else "") + ".jar"
    (directory / name).write_bytes(payload)
    dependency_xml = "".join(
        f'<dependency><groupId>{g}</groupId><artifactId>{a}</artifactId><version>{v}</version>'
        '<scope>compile</scope></dependency>' for (g,a),v in (dependencies or {}).items())
    pom = (f'<project><modelVersion>4.0.0</modelVersion><groupId>{group}</groupId>'
           f'<artifactId>{artifact}</artifactId><version>{version}</version>'
           f'<dependencies>{dependency_xml}</dependencies></project>\n')
    (directory / f"{stem}.pom").write_text(pom, encoding="utf-8")
    for file in [directory / name, directory / f"{stem}.pom"]:
        data = file.read_bytes()
        for algorithm in ["sha1", "sha512"]:
            file.with_name(file.name + "." + algorithm).write_text(hashlib.new(algorithm, data).hexdigest())


def main() -> None:
    if os.environ.get("GITHUB_ACTIONS") != "true":
        raise SystemExit("Dependency staging is allowed only in GitHub Actions.")
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / "manifest/mods.json").read_text())
    entry = next(m for m in manifest["mods"] if m["id"] == "create")
    if entry["version_id"] != CREATE_VERSION_ID:
        raise SystemExit("Update and revalidate the pinned build dependency bundle before upgrading Create.")
    url = f"https://cdn.modrinth.com/data/LNytGWDc/versions/{CREATE_VERSION_ID}/create-1.20.1-6.0.8.jar"
    with urllib.request.urlopen(url, timeout=120) as response:
        bundle = response.read()
    if hashlib.sha512(bundle).hexdigest() != CREATE_SHA512:
        raise SystemExit("Create bundle SHA-512 mismatch.")
    repo = Path(os.environ["RUNNER_TEMP"]) / "dimensionworks-create-build-maven"
    with zipfile.ZipFile(io.BytesIO(bundle)) as archive:
        metadata = json.loads(archive.read("META-INF/jarjar/metadata.json"))
        found = set()
        for item in metadata["jars"]:
            group, artifact = item["identifier"]["group"], item["identifier"]["artifact"]
            coordinate = group, artifact
            if coordinate not in EXPECTED:
                continue
            version = item["version"]["artifactVersion"]
            if version != EXPECTED[coordinate]:
                raise SystemExit(f"Unexpected bundled dependency version: {coordinate} {version}")
            payload = archive.read(item["path"])
            with zipfile.ZipFile(io.BytesIO(payload)) as dependency:
                if dependency.testzip() is not None:
                    raise SystemExit(f"Corrupt dependency: {coordinate}")
            stage(repo, group, artifact, version, payload)
            found.add(coordinate)
        if found != set(EXPECTED):
            raise SystemExit("Incomplete Create dependency bundle.")
        # Same source entries as Create's slimJar task, without bundled jars.
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w") as slim:
            for info in archive.infolist():
                if not info.filename.startswith("META-INF/jarjar/"):
                    slim.writestr(info, archive.read(info))
        stage(repo, "com.simibubi.create", "create-1.20.1", "6.0.8-291", output.getvalue(), "slim", EXPECTED)
    # Register before project repositories; restrict raw pinned versions to this
    # repository so Gradle does not retry the expired upstream certificate.
    init = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle"))) / "init.d"
    init.mkdir(parents=True, exist_ok=True)
    coordinates = {**EXPECTED, ("com.simibubi.create", "create-1.20.1"): "6.0.8-291"}
    filters = "\n".join(f"includeVersion('{g}', '{a}', '{v}')" for (g,a),v in coordinates.items())
    (init / "dimensionworks-create-build.gradle").write_text(
        "allprojects { project ->\n"
        "  def dwRepo = repositories.maven {\n"
        f"    name = 'DimensionWorksVerifiedCreateBundle'; url = uri({json.dumps(repo.as_uri())})\n"
        "  }\n"
        "  repositories.exclusiveContent {\n"
        "    forRepositories(dwRepo)\n"
        f"    filter {{ {filters} }}\n"
        "  }\n"
        "}\n", encoding="utf-8")
    print("Pinned Create bundle SHA-512 and dependency versions verified; CI build repository ready.")


if __name__ == "__main__":
    main()
