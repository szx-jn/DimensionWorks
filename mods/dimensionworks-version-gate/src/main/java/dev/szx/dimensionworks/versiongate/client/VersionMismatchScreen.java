package dev.szx.dimensionworks.versiongate.client;

import dev.szx.dimensionworks.versiongate.VersionCheck;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VersionMismatchScreen extends Screen {
    private final VersionCheck check;
    private String status = "";
    private boolean launching;

    public VersionMismatchScreen(VersionCheck check) {
        super(Component.literal("DimensionWorks 版本检查"));
        this.check = check;
    }

    @Override
    protected void init() {
        int buttonWidth = 112;
        int gap = 8;
        int left = width / 2 - buttonWidth - gap / 2;
        int right = width / 2 + gap / 2;
        int buttonY = height / 2 + 64;
        if (check.status() == VersionCheck.Status.TOO_OLD) {
            addRenderableWidget(Button.builder(Component.literal("立即更新"), button -> launchUpdater())
                    .bounds(left, buttonY, buttonWidth, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal(
                check.status() == VersionCheck.Status.TOO_OLD ? "暂不更新" : "返回标题"), button -> {
            Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }).bounds(check.status() == VersionCheck.Status.TOO_OLD ? right : (width - buttonWidth) / 2,
                buttonY, buttonWidth, 20).build());
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
        graphics.drawCenteredString(font, Component.literal(check.message()), width / 2, top + 14, 0xFFFF5555);
        graphics.drawCenteredString(font, Component.literal("服务器版本： " + check.serverText()), width / 2, top + 42, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal("你的版本： " + check.localText()), width / 2, top + 60, 0xFFFFFFFF);
        if (check.status() != VersionCheck.Status.TOO_OLD) {
            String help = check.status() == VersionCheck.Status.LOCAL_VERSION_ERROR
                    ? "请检查游戏根目录 VERSION 文件；详情见日志"
                    : "请联系管理员检查服务器根目录 VERSION 文件";
            graphics.drawCenteredString(font, Component.literal(help), width / 2, top + 84, 0xFFFFAA00);
        }
        if (!status.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal(status), width / 2, top + boxHeight + 3, 0xFFFFAA00);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void launchUpdater() {
        if (launching || check.status() != VersionCheck.Status.TOO_OLD) return;
        launching = true;
        setButtonsActive(false);
        Path gameDir = ClientVersionGate.gameDirectory();
        Minecraft client = Minecraft.getInstance();
        status = "正在查找本地更新器并启动终端...";
        CompletableFuture.runAsync(() -> {
            try {
                LocalUpdaterLauncher.launch(gameDir, check.serverText(), () -> client.execute(() -> {
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
