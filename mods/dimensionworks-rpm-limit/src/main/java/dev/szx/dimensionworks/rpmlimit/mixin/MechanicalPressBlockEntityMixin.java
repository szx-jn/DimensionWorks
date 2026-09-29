package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Applies output failure to world and belt pressing without changing its animation cycle. */
@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
public abstract class MechanicalPressBlockEntityMixin {

    @Inject(method = "tryProcessInWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$failWorldPress(ItemEntity itemEntity, boolean simulate,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (simulate)
            return;
        MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) (Object) this;
        if (!GearHeartEffects.shouldFailOutput(press))
            return;
        GearHeartEffects.consumeFailedWorldPress(itemEntity, press.canProcessInBulk());
        cir.setReturnValue(true);
    }

    @Inject(method = "tryProcessOnBelt", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$failBeltPress(TransportedItemStack input, List<ItemStack> outputList,
                                              boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate)
            return;
        MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) (Object) this;
        if (!GearHeartEffects.shouldFailOutput(press))
            return;
        cir.setReturnValue(true);
    }
}
