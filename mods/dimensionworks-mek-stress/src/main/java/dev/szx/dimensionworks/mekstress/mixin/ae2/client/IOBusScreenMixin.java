package dev.szx.dimensionworks.mekstress.mixin.ae2.client;

import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.IOBusScreen;
import dev.szx.dimensionworks.mekstress.card.StressOutputSettings;
import dev.szx.dimensionworks.mekstress.core.StressOutputMenuAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AEBaseScreen.class, remap = false)
public abstract class IOBusScreenMixin {

    @Unique
    private AETextField dimensionworks$rpmField;

    @Unique
    private AETextField dimensionworks$stressField;

    @Unique
    private Button dimensionworks$confirmButton;

    @Unique
    private int dimensionworks$panelX;

    @Unique
    private int dimensionworks$panelY;

    @Unique
    private static final int dimensionworks$PANEL_WIDTH = 84;

    @Unique
    private static final int dimensionworks$PANEL_HEIGHT = 72;

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void dimensionworks$initPanel(CallbackInfo ci) {
        if (!dimensionworks$isBusScreen()) {
            return;
        }
        AEBaseScreenAccessor screen = dimensionworks$screen();
        dimensionworks$panelX = Math.max(4, screen.dimensionworks$getGuiLeft() - dimensionworks$PANEL_WIDTH - 8);
        dimensionworks$panelY = screen.dimensionworks$getGuiTop() + 8;

        dimensionworks$rpmField = new AETextField(
            screen.dimensionworks$getStyle(), dimensionworks$screenAccess().dimensionworks$getFont(),
            dimensionworks$panelX + 4, dimensionworks$panelY + 14, 76, 14
        );
        dimensionworks$rpmField.setMaxLength(10);
        dimensionworks$rpmField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,10}"));
        dimensionworks$rpmField.setValue("32");
        dimensionworks$addWidget(dimensionworks$rpmField);

        dimensionworks$stressField = new AETextField(
            screen.dimensionworks$getStyle(), dimensionworks$screenAccess().dimensionworks$getFont(),
            dimensionworks$panelX + 4, dimensionworks$panelY + 42, 76, 14
        );
        dimensionworks$stressField.setMaxLength(12);
        dimensionworks$stressField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,12}"));
        dimensionworks$stressField.setValue("1024");
        dimensionworks$addWidget(dimensionworks$stressField);

        dimensionworks$confirmButton = Button.builder(
                Component.translatable("gui.dimensionworks_mek_stress.apply"),
                button -> dimensionworks$submit())
            .bounds(dimensionworks$panelX + 4, dimensionworks$panelY + 57, 76, 13)
            .build();
        dimensionworks$addWidget(dimensionworks$confirmButton);
        dimensionworks$refreshVisibility();
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void dimensionworks$updatePanel(CallbackInfo ci) {
        if (!dimensionworks$isBusScreen()) {
            return;
        }
        dimensionworks$refreshVisibility();
        StressOutputMenuAccess access = dimensionworks$menuAccess();
        if (!access.dimensionworks$hasStressCard()) {
            return;
        }
        if (!dimensionworks$rpmField.isFocused()) {
            dimensionworks$rpmField.setValue(Integer.toString(access.dimensionworks$getStressRpm()));
        }
        if (!dimensionworks$stressField.isFocused()) {
            dimensionworks$stressField.setValue(Long.toString(access.dimensionworks$getStressLimit()));
        }
    }

    @Inject(method = "render", at = @At("HEAD"), remap = true)
    private void dimensionworks$drawPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                          CallbackInfo ci) {
        if (!dimensionworks$isBusScreen()) {
            return;
        }
        if (!dimensionworks$menuAccess().dimensionworks$hasStressCard()) {
            return;
        }
        int left = dimensionworks$panelX;
        int top = dimensionworks$panelY;
        graphics.fill(left, top, left + dimensionworks$PANEL_WIDTH, top + dimensionworks$PANEL_HEIGHT, 0xDD202020);
        graphics.fill(left, top, left + dimensionworks$PANEL_WIDTH, top + 1, 0xFF606060);
        graphics.fill(left, top + dimensionworks$PANEL_HEIGHT - 1, left + dimensionworks$PANEL_WIDTH,
            top + dimensionworks$PANEL_HEIGHT, 0xFF606060);
        graphics.fill(left, top, left + 1, top + dimensionworks$PANEL_HEIGHT, 0xFF606060);
        graphics.fill(left + dimensionworks$PANEL_WIDTH - 1, top, left + dimensionworks$PANEL_WIDTH,
            top + dimensionworks$PANEL_HEIGHT, 0xFF606060);
        graphics.drawString(dimensionworks$screenAccess().dimensionworks$getFont(),
            Component.translatable("gui.dimensionworks_mek_stress.rpm"),
            left + 4, top + 4, 0xFFFFFFFF, false);
        graphics.drawString(dimensionworks$screenAccess().dimensionworks$getFont(),
            Component.translatable("gui.dimensionworks_mek_stress.stress"),
            left + 4, top + 32, 0xFFFFFFFF, false);
    }

    @Unique
    private void dimensionworks$refreshVisibility() {
        boolean visible = dimensionworks$rpmField != null
            && dimensionworks$menuAccess().dimensionworks$hasStressCard();
        if (dimensionworks$rpmField != null) {
            dimensionworks$rpmField.setVisible(visible);
        }
        if (dimensionworks$stressField != null) {
            dimensionworks$stressField.setVisible(visible);
        }
        if (dimensionworks$confirmButton != null) {
            dimensionworks$confirmButton.visible = visible;
            dimensionworks$confirmButton.active = visible;
        }
    }

    @Unique
    private void dimensionworks$submit() {
        int rpm;
        long stress;
        try {
            rpm = Integer.parseInt(dimensionworks$rpmField.getValue());
            stress = Long.parseLong(dimensionworks$stressField.getValue());
        } catch (NumberFormatException ignored) {
            return;
        }
        dimensionworks$menuAccess().dimensionworks$requestStressSettings(new StressOutputSettings(rpm, stress));
    }

    @Unique
    private AEBaseScreenAccessor dimensionworks$screen() {
        return (AEBaseScreenAccessor) (Object) this;
    }

    @Unique
    private boolean dimensionworks$isBusScreen() {
        return (Object) this instanceof IOBusScreen;
    }

    @Unique
    private ScreenAccessor dimensionworks$screenAccess() {
        return (ScreenAccessor) (Object) this;
    }

    @Unique
    private AbstractContainerMenu dimensionworks$containerMenu() {
        return ((AbstractContainerScreenInvoker) (Object) this).dimensionworks$getMenu();
    }

    @Unique
    private StressOutputMenuAccess dimensionworks$menuAccess() {
        return (StressOutputMenuAccess) (Object) dimensionworks$containerMenu();
    }

    @Unique
    private void dimensionworks$addWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        ScreenAccessor access = dimensionworks$screenAccess();
        access.dimensionworks$getRenderables().add(widget);
        access.dimensionworks$getChildren().add(widget);
        access.dimensionworks$getNarratables().add(widget);
    }
}
