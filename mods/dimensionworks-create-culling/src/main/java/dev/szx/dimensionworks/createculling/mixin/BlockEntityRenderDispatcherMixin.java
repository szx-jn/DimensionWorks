package dev.szx.dimensionworks.createculling.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.szx.dimensionworks.createculling.ClientOcclusion;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void dimensionworks$skipHidden(E be, float partialTick, PoseStack pose,
        MultiBufferSource buffers, CallbackInfo ci) {
        if (ClientOcclusion.skip(be)) ci.cancel();
    }
}
