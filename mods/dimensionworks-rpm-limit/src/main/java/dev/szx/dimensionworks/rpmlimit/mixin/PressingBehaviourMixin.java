package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.kinetics.press.PressingBehaviour.PressingBehaviourSpecifics;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Batches the world-item press call without advancing the press-head animation multiple times. */
@Mixin(value = PressingBehaviour.class, remap = false)
public abstract class PressingBehaviourMixin {

    @Redirect(
        method = "applyInWorld",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/press/PressingBehaviour$PressingBehaviourSpecifics;tryProcessInWorld(Lnet/minecraft/world/entity/item/ItemEntity;Z)Z"
        ),
        remap = false
    )
    private boolean dimensionworks$overspeedWorldPress(PressingBehaviourSpecifics specifics,
                                                     ItemEntity itemEntity, boolean simulate) {
        return OverspeedBonus.tryProcessWorldBatched(
            (PressingBehaviour) (Object) this,
            specifics,
            itemEntity,
            simulate
        );
    }
}
