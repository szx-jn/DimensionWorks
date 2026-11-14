package dev.szx.dimensionworks.transfer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.szx.dimensionworks.transfer.client.ElasticParcelClient;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "de.theidler.create_mobile_packages.PlayerPackClientEvents", remap = false)
public abstract class ElasticParcelRenderMixin {
    @Redirect(method = "renderBox", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/logistics/box/PackageRenderer;renderBox(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILdev/engine_room/flywheel/lib/model/baked/PartialModel;)V"), remap = false)
    private static void dimensionworks$slimePalette(Entity entity, float yaw, PoseStack pose, MultiBufferSource buffers,
                                                  int light, PartialModel model) {
        PackageRenderer.renderBox(entity, yaw, pose,
            entity instanceof Player player && ElasticParcelState.isElastic(player) ? ElasticParcelClient.slimeBuffers(buffers) : buffers,
            light, model);
    }
}
