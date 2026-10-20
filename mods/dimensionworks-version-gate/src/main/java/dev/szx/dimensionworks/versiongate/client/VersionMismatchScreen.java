package dev.szx.dimensionworks.versiongate.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

public final class VersionMismatchScreen extends Screen {
    private final String localVersion;
    private final String serverVersion;
    private String status = "";

    public VersionMismatchScreen(String localVersion, String serverVersion) {
        super(Component.literal("DimensionWorks 版本不匹配"));
        this.localVersion = localVersion;
        this.serverVersion = serverVersion;
    }

    @Override
    protected void init() {
        int buttonWidth = 112;
        int gap = 8;
        int left = width / 2 - buttonWidth - gap / 2;
        int right = width / 2 + gap / 2;
        int buttonY = height / 2 + 64;
        addRenderableWidget(Button.builder(Component.literal("立即更新"), button -> launchUpdater())
                .bounds(left, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("暂不更新"), button -> {
            Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }).bounds(right, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderDirtBackground(graphics);
        int boxWidth = Math.min(330, width - 24);
        int boxHeight = 116;
        int left = (width - boxWidth) / 2;
        int top = height / 2 - 78;
        graphics.fill(left, top, left + boxWidth, top + boxHeight, 0xE0101010);
        graphics.fill(left, top, left + boxWidth, top + 2, 0xFF8B8B8B);
        graphics.fill(left, top + boxHeight - 2, left + boxWidth, top + boxHeight, 0xFF404040);
        graphics.drawCenteredString(font, Component.literal("整合包版本过低，无法加入服务器"), width / 2, top + 14, 0xFFFF5555);
        graphics.drawCenteredString(font, Component.literal("服务器版本： " + serverVersion), width / 2, top + 42, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal("你的版本： " + localVersion), width / 2, top + 60, 0xFFFFFFFF);
        if (!status.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal(status), width / 2, top + boxHeight + 3, 0xFFFFAA00);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void launchUpdater() {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        status = "正在准备更新器...";
        CompletableFuture.runAsync(() -> {
            try {
                Path updater = downloadUpdater(gameDir);
                String tag = "v" + serverVersion;
                ProcessBuilder process;
                if (isWindows()) {
                    process = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "\"" + updater + "\"",
                            "--release", tag);
                } else {
                    process = new ProcessBuilder("bash", updater.toString(), "--release", tag);
                }
                process.directory(gameDir.toFile()).start();
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            } catch (IOException exception) {
                Minecraft.getInstance().execute(() -> status = "启动更新失败：" + exception.getMessage());
            }
        });
    }

    private Path downloadUpdater(Path gameDir) throws IOException {
        String extension = isWindows() ? "bat" : "sh";
        String filename = "update-dimensionworks-client." + extension;
        String baseUrl = "https://github.com/szx-jn/DimensionWorks/releases/download/v"
                + serverVersion + "/";
        Path destination = gameDir.resolve(filename);
        Path temporary = gameDir.resolve(filename + ".download");
        Path checksums = gameDir.resolve("SHA256SUMS." + serverVersion + ".download");
        try {
            download(baseUrl + "SHA256SUMS", checksums);
            download(baseUrl + filename, temporary);
            String expected = expectedChecksum(Files.readString(checksums), filename);
            String actual = sha256(temporary);
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IOException("更新脚本 SHA-256 校验失败，已拒绝执行。");
            }
            try {
                return Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                return Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(checksums);
        }
    }

    private static void download(String url, Path destination) throws IOException {
        java.net.URLConnection connection = java.net.URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(20_000);
        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
    }

    private static String expectedChecksum(String checksums, String filename) throws IOException {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?m)^([0-9a-fA-F]{64})\\s+\\*?" + java.util.regex.Pattern.quote(filename) + "$");
        java.util.regex.Matcher matcher = pattern.matcher(checksums);
        if (!matcher.find()) {
            throw new IOException("SHA256SUMS 中没有对应更新脚本的校验值。");
        }
        return matcher.group(1);
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }
}
