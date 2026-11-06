package dev.szx.dimensionworks.versiongate.client;

import java.io.IOException;
import java.nio.file.Path;
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
    private boolean launching;

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
        setButtonsActive(!launching);
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
        if (launching) return;
        launching = true;
        setButtonsActive(false);
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Minecraft client = Minecraft.getInstance();
        status = "正在查找本地更新器并启动终端...";
        CompletableFuture.runAsync(() -> {
            try {
                LocalUpdaterLauncher.launch(gameDir, serverVersion, () -> client.execute(() -> {
                    status = "更新终端已就绪，正在正常关闭游戏...";
                    client.stop();
                }));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                client.execute(() -> launchFailed("更新器启动被中断。"));
            } catch (IOException | RuntimeException exception) {
                client.execute(() -> launchFailed(exception.getMessage()));
            }
        });
    }

    private void setButtonsActive(boolean active) {
        for (var child : children()) {
            if (child instanceof Button button) button.active = active;
        }
    }

    private void launchFailed(String reason) {
        launching = false;
        setButtonsActive(true);
        status = "启动更新失败：" + (reason == null ? "请检查本地更新器与终端。" : reason);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!launching) Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }
}
