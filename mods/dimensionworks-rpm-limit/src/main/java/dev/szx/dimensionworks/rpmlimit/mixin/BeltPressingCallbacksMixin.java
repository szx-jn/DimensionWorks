package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.BeltPressingCallbacks;
import com.simibubi.create.content.kinetics.press.PressingBehaviour.PressingBehaviourSpecifics;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Aggregates depot/belt press operations into the same animation cycle. */
@Mixin(value = BeltPressingCallbacks.class, remap = false)
public abstract class BeltPressingCallbacksMixin {

    @Redirect(
        method = "whenItemHeld",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/press/PressingBehaviour$PressingBehaviourSpecifics;tryProcessOnBelt(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Ljava/util/List;Z)Z"
        ),
        remap = false
    )
    private static boolean dimensionworks$overspeedBeltPress(PressingBehaviourSpecifics specifics,
                                                              TransportedItemStack transported,
                                                              List<ItemStack> outputList,
                                                              boolean simulate) {
        return OverspeedBonus.tryProcessOnBeltBatched(specifics, transported, outputList, simulate);
    }
}
