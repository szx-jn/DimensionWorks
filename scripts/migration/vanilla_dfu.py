"""复用单个 Java 21 进程执行唯一旧档的原版 3465 → 3955 DFU。"""
from __future__ import annotations

import base64
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile

import nbtlib as n


class VanillaDfu:
    def __init__(self, classpath_file: Path, java_home: Path, log: Path):
        self.classpath = os.pathsep.join(classpath_file.read_text().splitlines())
        self.java_home = java_home
        self.log = log
        self.sequence = 0
        self.process = None
        self.folder = None
        self.log_stream = None

    def __enter__(self):
        self.folder = tempfile.TemporaryDirectory(prefix="dimensionworks-vanilla-dfu-")
        try:
            self.log_stream = open(self.log, "x", opener=lambda path, flags: os.open(path, flags, 0o600))
            subprocess.run([str(self.java_home / "bin/javac"), "--release", "21", "-proc:none",
                            "-cp", self.classpath, "-d", self.folder.name,
                            str(Path(__file__).parent / "java/VanillaDfuBridge.java")],
                           stdout=self.log_stream, stderr=subprocess.STDOUT, check=True)
            self.process = subprocess.Popen([
                str(self.java_home / "bin/java"), "-Xmx512M", "-XX:ActiveProcessorCount=2",
                "-cp", self.folder.name + os.pathsep + self.classpath,
                "dev.szx.dimensionworks.migration.VanillaDfuBridge"],
                stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.log_stream, text=True)
            if self._read("DW_DFU_READY\t") != "3955":
                raise ValueError("原版 DFU 目标版本不符")
            return self
        except BaseException:
            self.close()
            raise

    def _read(self, prefix: str) -> str:
        for line in self.process.stdout:
            if line.startswith(prefix):
                return line[len(prefix):].rstrip("\n")
            self.log_stream.write(line)
            self.log_stream.flush()
        raise RuntimeError(f"原版 DFU 进程退出；详情见私有日志 {self.log}")

    def update(self, value: n.Compound, reference: str) -> n.Compound:
        if not isinstance(value, n.Compound):
            raise ValueError("DFU 输入必须是复合 NBT")
        self.sequence += 1
        raw = io.BytesIO()
        n.File(value).write(raw)
        self.process.stdin.write(json.dumps({"sequence": self.sequence, "reference": reference,
                                            "nbt": base64.b64encode(raw.getvalue()).decode()}) + "\n")
        self.process.stdin.flush()
        result = json.loads(self._read("DW_DFU_RESULT\t"))
        if result["sequence"] != self.sequence:
            raise RuntimeError("DFU 响应顺序不符")
        if "error" in result:
            raise ValueError(result["error"])
        return n.Compound(n.File.parse(io.BytesIO(base64.b64decode(result["nbt"], validate=True))))

    def close(self):
        code = None
        if self.process:
            if self.process.stdin:
                self.process.stdin.close()
            try:
                self.process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait()
            self.process.stdout.close()
            code = self.process.returncode
            self.process = None
        if self.log_stream:
            self.log_stream.close()
            self.log_stream = None
        if self.folder:
            self.folder.cleanup()
            self.folder = None
        return code

    def __exit__(self, exc_type, exc_value, traceback):
        code = self.close()
        if exc_type is None and code != 0:
            raise RuntimeError(f"原版 DFU 进程未正常退出：{code}")
