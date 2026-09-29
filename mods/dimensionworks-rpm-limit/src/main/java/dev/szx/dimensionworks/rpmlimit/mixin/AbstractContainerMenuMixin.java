package dev.szx.dimensionworks.rpmlimit.mixin;

import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents inventory manipulation from non-packet paths during the freeze. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$freezeMenuClick(int slotId, int dragType,
                                                net.minecraft.world.inventory.ClickType clickType,
                                                net.minecraft.world.entity.player.Player player,
                                                CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && GearHeartEffects.isFrozen(serverPlayer))
            ci.cancel();
    }
}
