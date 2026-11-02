package dev.szx.dimensionworks.jeiticks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

public record FuelIcon(ItemStack stack, ResourceLocation sprite, int tint, Component name) {
    public static FuelIcon item(ItemStack stack) {
        return new FuelIcon(stack.copy(), null, 0xFFFFFF, stack.getHoverName());
    }

    public static FuelIcon item(ResourceLocation id) {
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return null;
        }
        var item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? null : item(new ItemStack(item));
    }

    public static FuelIcon fluid(Fluid fluid) {
        FluidStack contents = new FluidStack(fluid, 1000);
        ItemStack bucket = FluidUtil.getFilledBucket(contents);
        if (!bucket.isEmpty()) {
            return new FuelIcon(bucket, null, 0xFFFFFF, contents.getDisplayName());
        }
        var extension = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        return new FuelIcon(ItemStack.EMPTY, extension.getStillTexture(contents), extension.getTintColor(contents),
            contents.getDisplayName());
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
        } else if (sprite != null) {
            graphics.flush();
            RenderSystem.setShaderColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F,
                (tint & 255) / 255F, 1);
            try {
                var texture = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(sprite);
                graphics.blit(x, y, 0, 16, 16, texture);
                graphics.flush();
            } finally {
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }
    }
}
