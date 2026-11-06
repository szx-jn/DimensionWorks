package dev.szx.dimensionworks.versiongate.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Opens a local updater in an independent terminal, after this JVM has exited normally. */
public final class LocalUpdaterLauncher {
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(10);
    private static final List<String> LINUX_TERMINALS = List.of("x-terminal-emulator", "gnome-terminal",
        "konsole", "xfce4-terminal", "mate-terminal", "kitty", "alacritty", "xterm");

    enum Platform {
        WINDOWS, MACOS, LINUX;

        static Platform detect(String osName) throws IOException {
            String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
            // Darwin contains "win", so it must be recognized before Windows.
            if (os.contains("mac") || os.contains("darwin")) return MACOS;
            if (os.contains("windows")) return WINDOWS;
            if (os.contains("linux")) return LINUX;
            throw new IOException("当前操作系统暂不支持自动更新：" + osName);
        }

        String updaterFilename(String architecture) throws IOException {
            String arch = architecture == null ? "" : architecture.toLowerCase(Locale.ROOT);
            boolean amd64 = arch.equals("amd64") || arch.equals("x86_64");
            boolean arm64 = arch.equals("arm64") || arch.equals("aarch64");
            if (this == MACOS && arm64) return "update-dimensionworks-client-macos-arm64";
            if (this == WINDOWS && amd64) return "update-dimensionworks-client-windows-amd64.exe";
            if (this == LINUX && amd64) return "update-dimensionworks-client-linux-amd64";
            throw new IOException("当前 " + this + " 架构暂不支持自动更新：" + architecture);
        }
    }

    record LaunchPlan(List<String> command, Path directory, Path ready, Path script, Path log) {
        void cleanup() throws IOException {
            Files.deleteIfExists(ready);
            if (script != null) Files.deleteIfExists(script);
        }
    }

    private LocalUpdaterLauncher() {}

    public static void launch(Path gameDirectory, String serverVersion, Runnable onReady)
            throws IOException, InterruptedException {
        Platform platform = Platform.detect(System.getProperty("os.name"));
        Path directory = gameDirectory.toAbsolutePath().normalize();
        Path updater = findUpdater(directory, platform, System.getProperty("os.arch"));
        if (platform != Platform.WINDOWS && !Files.isExecutable(updater)
                && !updater.toFile().setExecutable(true, true)) {
            throw new IOException("无法设置更新器执行权限：" + updater.getFileName());
        }
        Path terminal = platform == Platform.LINUX ? findLinuxTerminal(System.getenv("PATH")) : null;
        LaunchPlan plan = prepare(directory, updater, platform, serverVersion,
            ProcessHandle.current().pid(), terminal);
        start(plan, onReady, READY_TIMEOUT);
    }

    static Path findUpdater(Path directory, Platform platform, String architecture) throws IOException {
        Path updater = directory.resolve(platform.updaterFilename(architecture));
        if (!Files.isRegularFile(updater) || !Files.isReadable(updater) || Files.size(updater) == 0) {
            throw new IOException("游戏目录未找到可用更新器：" + updater.getFileName()
                + "，请先将对应平台的客户端更新器放入游戏目录。");
        }
        return updater;
    }

    static LaunchPlan prepare(Path directory, Path updater, Platform platform, String version,
            long gamePid, Path linuxTerminal) throws IOException {
        if (version == null || !version.matches("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)")) {
            throw new IOException("服务器版本号无效，无法启动更新器。");
        }
        if (gamePid <= 0) throw new IOException("无法确定当前游戏进程。");
        Path handoff = directory.resolve(".dimensionworks-update/handoff");
        Files.createDirectories(handoff);
        String id = "version-gate-" + UUID.randomUUID();
        Path ready = handoff.resolve(id + ".ready");
        Path log = handoff.resolve(id + ".log");
        if (platform == Platform.WINDOWS) {
            String inner = windowsHandoff(directory, updater, "v" + version, gamePid, ready);
            String outer = "$ErrorActionPreference = 'Stop'; Start-Process -FilePath 'powershell.exe' "
                + "-WorkingDirectory " + powershellQuote(directory.toString())
                + " -ArgumentList @('-NoLogo','-NoProfile','-NoExit','-EncodedCommand',"
                + powershellQuote(encodedPowerShell(inner)) + ")";
            return new LaunchPlan(List.of("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                "-EncodedCommand", encodedPowerShell(outer)), directory, ready, null, log);
        }
        Path script = handoff.resolve(id + ".sh");
        Files.writeString(script, unixHandoff(directory, updater, "v" + version, gamePid, ready), StandardCharsets.UTF_8);
        List<String> command;
        if (platform == Platform.MACOS) {
            String shell = "exec /bin/sh " + shellQuote(script.toString());
            command = List.of("/usr/bin/osascript", "-e", "tell application \"Terminal\"",
                "-e", "do script " + appleScriptQuote(shell), "-e", "activate", "-e", "end tell");
        } else {
            command = linuxTerminalCommand(linuxTerminal, script);
        }
        return new LaunchPlan(command, directory, ready, script, log);
    }

    static void start(LaunchPlan plan, Runnable onReady, Duration timeout) throws IOException, InterruptedException {
        Process process = null;
        boolean ready = false;
        try {
            process = new ProcessBuilder(plan.command()).directory(plan.directory().toFile())
                .redirectErrorStream(true).redirectOutput(plan.log().toFile()).start();
            // The new terminal owns its own stdin; no game pipe should keep it dependent on this JVM.
            process.getOutputStream().close();
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                if (Files.isRegularFile(plan.ready())) {
                    plan.cleanup();
                    ready = true;
                    onReady.run();
                    return;
                }
                if (!process.isAlive() && process.exitValue() != 0) {
                    throw new IOException("终端启动失败（退出码 " + process.exitValue() + "）：" + launcherError(plan.log()));
                }
                Thread.sleep(50);
            }
            throw new IOException("终端未确认启动，请检查终端权限；游戏尚未退出。");
        } finally {
            if (!ready) {
                if (process != null && process.isAlive()) process.destroy();
                plan.cleanup();
            }
        }
    }

    private static String launcherError(Path log) throws IOException {
        try (var input = Files.newInputStream(log)) {
            String text = new String(input.readNBytes(4096), StandardCharsets.UTF_8)
                .replaceAll("[\\r\\n\\p{Cntrl}]+", " ").trim();
            return text.isEmpty() ? "没有可用的终端窗口" : text;
        }
    }

    static String unixHandoff(Path directory, Path updater, String tag, long gamePid, Path ready) {
        return "#!/bin/sh\n"
            + "printf '%s\\n' '就绪' > " + shellQuote(ready.toString()) + " || exit 1\n"
            + "rm -f -- \"$0\"\n"
            + "printf '%s\\n' '正在等待 Minecraft 正常退出…'\n"
            + "remaining=120\n"
            + "while kill -0 " + gamePid + " 2>/dev/null; do\n"
            + "  if [ \"$remaining\" -le 0 ]; then\n"
            + "    printf '%s\\n' 'Minecraft 尚未退出，已取消更新。按回车关闭。'\n"
            + "    read -r answer\n    exit 1\n  fi\n"
            + "  sleep 1\n  remaining=$((remaining - 1))\ndone\n"
            + "cd " + shellQuote(directory.toString()) + " || exit 1\n"
            + "exec " + shellQuote(updater.toString()) + " --release " + shellQuote(tag) + "\n";
    }

    static String windowsHandoff(Path directory, Path updater, String tag, long gamePid, Path ready) {
        return "$ErrorActionPreference = 'Stop'; "
            + "Set-Content -LiteralPath " + powershellQuote(ready.toString()) + " -Value 'ready'; "
            + "Write-Host '正在等待 Minecraft 正常退出…'; "
            + "if (Get-Process -Id " + gamePid + " -ErrorAction SilentlyContinue) { "
            + "Wait-Process -Id " + gamePid + " -Timeout 120 -ErrorAction SilentlyContinue }; "
            + "if (Get-Process -Id " + gamePid + " -ErrorAction SilentlyContinue) { "
            + "Write-Host 'Minecraft 尚未退出，已取消更新。'; exit 1 }; "
            + "Set-Location -LiteralPath " + powershellQuote(directory.toString()) + "; "
            + "& " + powershellQuote(updater.toString()) + " --release " + powershellQuote(tag);
    }

    static Path findLinuxTerminal(String searchPath) throws IOException {
        for (String name : LINUX_TERMINALS) {
            if (searchPath == null) break;
            for (String entry : searchPath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                if (entry.isBlank()) continue;
                Path path = Path.of(entry).resolve(name);
                if (Files.isRegularFile(path) && Files.isExecutable(path)) return path.toAbsolutePath();
            }
        }
        throw new IOException("未找到可用 Linux 终端，请安装终端或手动运行游戏目录中的更新器。");
    }

    static List<String> linuxTerminalCommand(Path terminal, Path script) throws IOException {
        if (terminal == null) throw new IOException("未找到可用 Linux 终端。");
        var command = new ArrayList<String>();
        command.add(terminal.toString());
        switch (terminal.getFileName().toString()) {
            case "gnome-terminal" -> command.add("--");
            case "konsole" -> { command.add("--separate"); command.add("-e"); }
            case "xfce4-terminal" -> { command.add("--disable-server"); command.add("-x"); }
            case "mate-terminal" -> command.add("-x");
            case "kitty" -> {}
            default -> command.add("-e");
        }
        command.add("/bin/sh");
        command.add(script.toString());
        return List.copyOf(command);
    }

    static String shellQuote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
    static String powershellQuote(String value) { return "'" + value.replace("'", "''") + "'"; }
    static String appleScriptQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    static String encodedPowerShell(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_16LE));
    }
}
