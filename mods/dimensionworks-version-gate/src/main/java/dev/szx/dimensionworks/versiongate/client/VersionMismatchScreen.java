package dev.szx.dimensionworks.versiongate.client;

import java.io.IOException;
import java.io.InputStream;
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
                Path updater;
                try {
                    // Prefer the updater shipped with the server's exact release.
                    updater = downloadUpdater(gameDir);
                } catch (IOException downloadFailure) {
                    updater = findUpdater(gameDir);
                    if (updater == null) throw downloadFailure;
                }
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
        String url = "https://github.com/szx-jn/DimensionWorks/releases/download/v"
                + serverVersion + "/" + filename;
        Path destination = gameDir.resolve(filename);
        Path temporary = gameDir.resolve(filename + ".download");
        try (InputStream input = java.net.URI.create(url).toURL().openStream()) {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw new IOException("无法从对应版本下载更新脚本", exception);
        }
        try {
            return Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            return Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path findUpdater(Path gameDir) {
        Path rootScript = gameDir.resolve("update-dimensionworks-client." + (isWindows() ? "bat" : "sh"));
        if (Files.isRegularFile(rootScript)) return rootScript;
        Path nestedScript = gameDir.resolve("release-updaters")
                .resolve("update-dimensionworks-client." + (isWindows() ? "bat" : "sh"));
        return Files.isRegularFile(nestedScript) ? nestedScript : null;
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
