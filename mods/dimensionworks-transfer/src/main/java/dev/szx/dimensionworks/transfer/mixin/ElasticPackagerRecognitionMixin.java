package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.elastic.ElasticPackager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "de.theidler.create_mobile_packages.CommonEvents", remap = false)
public abstract class ElasticPackagerRecognitionMixin {
    @Inject(method = "isHoldingPackager", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dimensionworks$recognizeElasticTool(Player player, CallbackInfoReturnable<Boolean> ci) {
        if (player.getMainHandItem().getItem() instanceof ElasticPackager
            || player.getOffhandItem().getItem() instanceof ElasticPackager) ci.setReturnValue(true);
    }
}
