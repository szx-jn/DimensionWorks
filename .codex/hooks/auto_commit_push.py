#!/usr/bin/env python3
"""Codex Stop hook that commits and pushes completed changes.

The hook intentionally runs only for the root Stop event. It never blocks the
agent turn: failures are returned as a concise systemMessage so the user sees
what failed without extending or retrying the turn.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

try:
    import fcntl
except ImportError:  # pragma: no cover - Windows fallback is best effort.
    fcntl = None


COMMIT_MESSAGE = "chore(auto): commit completed changes"
GIT_TIMEOUT_SECONDS = 120
MAX_ERROR_OUTPUT_CHARS = 1800


@dataclass
class CommandResult:
    returncode: int
    stdout: str
    stderr: str
    error: str | None = None

    def output(self) -> str:
        parts = []
        if self.stderr.strip():
            parts.append(self.stderr.strip())
        if self.stdout.strip():
            parts.append(self.stdout.strip())
        if self.error:
            parts.append(self.error)
        return "\n".join(parts)


class HookFailure(Exception):
    def __init__(self, stage: str, reason: str, output: str = "") -> None:
        super().__init__(reason)
        self.stage = stage
        self.reason = reason
        self.output = output


@dataclass
class Context:
    branch: str | None = None
    commit: str | None = None
    changed_files: int = 0
    made_commit: bool = False
    remote: str = "origin"


def run(
    cwd: Path,
    argv: list[str],
    *,
    timeout: int = GIT_TIMEOUT_SECONDS,
    env: dict[str, str] | None = None,
) -> CommandResult:
    merged_env = os.environ.copy()
    if env:
        merged_env.update(env)
    try:
        completed = subprocess.run(
            argv,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=timeout,
            env=merged_env,
        )
    except subprocess.TimeoutExpired as error:
        stdout = decode_timeout_stream(error.stdout)
        stderr = decode_timeout_stream(error.stderr)
        return CommandResult(
            returncode=124,
            stdout=stdout,
            stderr=stderr,
            error=f"command timed out after {timeout}s: {' '.join(argv)}",
        )
    except OSError as error:
        return CommandResult(
            returncode=127,
            stdout="",
            stderr="",
            error=f"cannot run {' '.join(argv)}: {error}",
        )
    return CommandResult(
        returncode=completed.returncode,
        stdout=completed.stdout,
        stderr=completed.stderr,
    )


def decode_timeout_stream(value: bytes | str | None) -> str:
    if value is None:
        return ""
    if isinstance(value, bytes):
        return value.decode("utf-8", errors="replace")
    return value


def git(root: Path, *arguments: str, env: dict[str, str] | None = None) -> CommandResult:
    return run(root, ["git", *arguments], env=env)


def require_git(
    root: Path,
    stage: str,
    *arguments: str,
    env: dict[str, str] | None = None,
) -> str:
    result = git(root, *arguments, env=env)
    if result.returncode != 0:
        raise HookFailure(stage, f"git {' '.join(arguments)} failed", result.output())
    return result.stdout.strip()


def repository_root(cwd: Path) -> Path:
    result = run(cwd, ["git", "rev-parse", "--show-toplevel"])
    if result.returncode != 0:
        raise HookFailure("仓库检查", "当前目录不是 Git 仓库", result.output())
    root = result.stdout.strip()
    if not root:
        raise HookFailure("仓库检查", "Git 未返回仓库根目录")
    return Path(root).resolve()


def git_path(root: Path, name: str) -> Path:
    value = require_git(root, "Git 状态", "rev-parse", "--git-path", name)
    path = Path(value)
    return path if path.is_absolute() else (root / path).resolve()


def ensure_no_operation_in_progress(root: Path) -> None:
    markers = (
        "MERGE_HEAD",
        "REBASE_HEAD",
        "CHERRY_PICK_HEAD",
        "REVERT_HEAD",
        "rebase-merge",
        "rebase-apply",
    )
    for marker in markers:
        if git_path(root, marker).exists():
            raise HookFailure("Git 状态", f"检测到未完成的 Git 操作：{marker}")


def current_branch(root: Path) -> str:
    result = git(root, "symbolic-ref", "--quiet", "--short", "HEAD")
    branch = result.stdout.strip()
    if result.returncode != 0 or not branch:
        raise HookFailure("Git 状态", "当前处于 detached HEAD，拒绝自动提交")
    return branch


def ensure_origin_exists(root: Path) -> None:
    result = git(root, "remote", "get-url", "origin")
    if result.returncode != 0 or not result.stdout.strip():
        raise HookFailure("远端检查", "没有配置名为 origin 的远端", result.output())


def ensure_no_conflicts(root: Path) -> None:
    result = git(root, "ls-files", "--unmerged")
    if result.returncode != 0:
        raise HookFailure("Git 状态", "无法检查未解决冲突", result.output())
    if result.stdout.strip():
        raise HookFailure("Git 状态", "存在未解决的 Git 冲突，拒绝自动提交")


def changed_path_count(root: Path) -> int:
    result = git(root, "status", "--porcelain=v1", "--untracked-files=all")
    if result.returncode != 0:
        raise HookFailure("Git 状态", "无法读取工作区状态", result.output())
    return len(result.stdout.splitlines())


def staged_path_count(root: Path) -> int:
    result = git(root, "diff", "--cached", "--name-only", "-z")
    if result.returncode != 0:
        raise HookFailure("git add", "无法统计已暂存文件", result.output())
    return len([item for item in result.stdout.split("\0") if item])


def current_head(root: Path) -> str:
    return require_git(root, "提交检查", "rev-parse", "HEAD")


def push_environment() -> dict[str, str]:
    environment = {
        "GIT_TERMINAL_PROMPT": "0",
        "GIT_ASKPASS": "/usr/bin/false",
        "SSH_ASKPASS": "/usr/bin/false",
    }
    if "GIT_SSH_COMMAND" not in os.environ:
        environment["GIT_SSH_COMMAND"] = "ssh -oBatchMode=yes -oConnectTimeout=15"
    return environment


def remote_tracking_head(root: Path, branch: str) -> str | None:
    reference = f"refs/remotes/origin/{branch}"
    result = git(root, "rev-parse", "--verify", reference)
    if result.returncode != 0:
        return None
    return result.stdout.strip() or None


def push_branch(root: Path, branch: str) -> None:
    target = f"HEAD:refs/heads/{branch}"
    result = git(
        root,
        "push",
        "--porcelain",
        "origin",
        target,
        env=push_environment(),
    )
    if result.returncode != 0:
        raise HookFailure("git push", f"推送 origin/{branch} 失败", result.output())


def acquire_lock(root: Path):
    lock_path = git_path(root, "codex-auto-commit-push.lock")
    lock_file = lock_path.open("a+", encoding="utf-8")
    if fcntl is None:
        return lock_file
    try:
        fcntl.flock(lock_file.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError as error:
        lock_file.close()
        raise HookFailure("并发检查", "另一个自动提交正在进行中") from error
    return lock_file


def release_lock(lock_file) -> None:
    try:
        if fcntl is not None:
            fcntl.flock(lock_file.fileno(), fcntl.LOCK_UN)
    finally:
        lock_file.close()


def truncate_output(output: str) -> str:
    output = output.strip()
    if len(output) <= MAX_ERROR_OUTPUT_CHARS:
        return output
    return "...\n" + output[-MAX_ERROR_OUTPUT_CHARS:]


def success_message(context: Context, body: list[str]) -> str:
    return "\n".join(
        [
            "自动提交成功",
            f"分支: {context.branch}",
            *body,
            f"远端: {context.remote}",
        ]
    )


def failure_message(error: HookFailure, context: Context) -> str:
    lines = [
        "自动提交失败",
        f"阶段: {error.stage}",
        f"原因: {error.reason}",
    ]
    if context.branch:
        lines.append(f"分支: {context.branch}")
    if context.commit:
        lines.append(f"本地提交: {context.commit}")
    if context.changed_files:
        lines.append(f"变更: {context.changed_files} 个文件")
    lines.append(f"远端: {context.remote}")
    output = truncate_output(error.output)
    if output:
        lines.append("Git 输出:")
        lines.extend(output.splitlines()[-20:])
    return "\n".join(lines)


def emit_system_message(message: str) -> None:
    payload = {
        "suppressOutput": True,
        "systemMessage": message,
    }
    json.dump(payload, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")


def perform(context: Context, root: Path) -> list[str]:
    ensure_no_operation_in_progress(root)
    context.branch = current_branch(root)
    ensure_origin_exists(root)
    ensure_no_conflicts(root)

    pending_paths = changed_path_count(root)
    if pending_paths:
        add_result = git(root, "add", "-A", "--")
        if add_result.returncode != 0:
            context.changed_files = pending_paths
            raise HookFailure("git add", "暂存工作区修改失败", add_result.output())

        context.changed_files = staged_path_count(root)
        if context.changed_files == 0:
            pass
        else:
            commit_result = git(root, "commit", "-m", COMMIT_MESSAGE)
            if commit_result.returncode != 0:
                raise HookFailure("git commit", "创建自动提交失败", commit_result.output())
            context.commit = current_head(root)
            context.made_commit = True

    if context.commit is None:
        context.commit = current_head(root)

    remote_head = remote_tracking_head(root, context.branch)
    if remote_head == context.commit:
        push_status = "无需推送（远端跟踪引用已同步）"
    else:
        push_branch(root, context.branch)
        push_status = "成功"

    if context.made_commit:
        body = [
            f"提交: {context.commit[:12]}",
            f"变更: {context.changed_files} 个文件",
            f"推送: {push_status}",
        ]
    else:
        body = [
            "状态: 本次没有新的工作区修改",
            f"最新提交: {context.commit[:12]}",
            f"推送: {push_status}",
        ]
    return body


def read_event() -> dict:
    try:
        event = json.load(sys.stdin)
    except (json.JSONDecodeError, OSError) as error:
        raise HookFailure("钩子输入", f"无法读取 Stop JSON：{error}") from error
    if not isinstance(event, dict):
        raise HookFailure("钩子输入", "Stop JSON 顶层必须是对象")
    return event


def event_cwd(event: dict) -> Path:
    value = event.get("cwd")
    if isinstance(value, str) and value:
        cwd = Path(value)
        if cwd.is_dir():
            return cwd
    return Path.cwd()


def main() -> int:
    try:
        event = read_event()
    except HookFailure as error:
        emit_system_message(failure_message(error, Context()))
        return 0

    if event.get("hook_event_name") != "Stop":
        return 0

    context = Context()
    try:
        root = repository_root(event_cwd(event))
        lock_file = acquire_lock(root)
        try:
            body = perform(context, root)
        finally:
            release_lock(lock_file)
    except HookFailure as error:
        emit_system_message(failure_message(error, context))
        return 0
    except Exception as error:  # Defensive: never turn a reporting failure into a blocked turn.
        emit_system_message(failure_message(HookFailure("钩子内部错误", str(error)), context))
        return 0

    emit_system_message(success_message(context, body))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
