#!/usr/bin/env python3
"""用固定 Minecraft/FTB 类路径编译并运行只读 SNBT 导出器。"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path)
    parser.add_argument("cache", type=Path)
    parser.add_argument("--classpath", type=Path, required=True, help="writeMigrationClasspath 生成的每行一个路径的文件")
    args = parser.parse_args()
    classpath = os.pathsep.join(args.classpath.read_text().splitlines())
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
    javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
    if not java or not javac:
        parser.error("需要 Java 21 JDK，并配置 JAVA_HOME 或 PATH")
    source = Path(__file__).parent / "java/ExportFtbSnbt.java"
    # 参数列表直接交给子进程，不经过 shell；编译产物只存在于私有临时目录。
    with tempfile.TemporaryDirectory(prefix="dimensionworks-ftb-snbt-") as folder:
        subprocess.run([javac, "--release", "21", "-proc:none", "-cp", classpath, "-d", folder, str(source)], check=True)
        subprocess.run([java, "-Xmx512M", "-cp", folder + os.pathsep + classpath,
                        "dev.szx.dimensionworks.migration.ExportFtbSnbt", str(args.world), str(args.cache)], check=True)


if __name__ == "__main__":
    main()
