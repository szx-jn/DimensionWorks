package dev.szx.dimensionworks.mekstress.client;

import appeng.client.render.tesr.CellLedRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.card.MemoryCardItem;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

/** Lights the first ten physical status windows, following AE2's two-column drive layout. */
public final class MemoryDriveLedRenderer implements BlockEntityRenderer<MemoryDriveBlockEntity> {
    public MemoryDriveLedRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MemoryDriveBlockEntity drive, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (drive.getLevel() == null) return;
        VertexConsumer out = buffers.getBuffer(CellLedRenderer.RENDER_LAYER);
        int count = Math.min(10, drive.tier().slotsPerDrive());
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = drive.getInternalInventory().getStackInSlot(slot);
            if (!(stack.getItem() instanceof MemoryCardItem card)) continue;
            int color = color(card.type());
            int col = slot % 2;
            int row = slot / 2;
            float x0 = (col == 0 ? 5.15f : 9.15f) / 16.0f;
            float y0 = (12.4f - row * 2.0f) / 16.0f;
            float x1 = x0 + 1.7f / 16.0f;
            float y1 = y0 + 1.0f / 16.0f;
            float z = 0.058f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            vertex(out, pose, x0, y1, z, r, g, b);
            vertex(out, pose, x1, y1, z, r, g, b);
            vertex(out, pose, x1, y0, z, r, g, b);
            vertex(out, pose, x0, y0, z, r, g, b);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z,
                               float r, float g, float b) {
        out.vertex(pose.last().pose(), x, y, z).color(r, g, b, 1.0f).endVertex();
    }

    private static int color(MemoryCardType type) {
        return switch (type) {
            case DEFECTIVE -> 0x969EAA;
            case ECONOMY -> 0x35D0A4;
            case BALANCED -> 0x31DAEF;
            case HIGH_SPEED -> 0xF640C6;
            case HIGH_STORAGE -> 0xB169F9;
            case FINAL -> 0xFFDE64;
        };
    }
}
