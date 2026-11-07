package dev.szx.dimensionworks.transfer.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Packed players follow the chain target without Create's spring inertia or distance ejection. */
@Mixin(value = ChainConveyorRidingHandler.class, remap = false)
abstract class PackedPlayerChainRidingMixin {
    @Unique
    private static boolean dimensionworks$isPackedRider() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && PackedPlayerTransport.ignoresBlocks(player);
    }

    @ModifyConstant(method = "clientTick", constant = @Constant(doubleValue = 0.75D), require = 1, allow = 1)
    private static double dimensionworks$removeInertia(double original) {
        return dimensionworks$isPackedRider() ? 0.0D : original;
    }

    // The first 0.25 is the chain wheel's target height; only change the movement weight.
    @ModifyConstant(method = "clientTick", constant = @Constant(doubleValue = 0.25D, ordinal = 1), require = 1, allow = 1)
    private static double dimensionworks$followTarget(double original) {
        return dimensionworks$isPackedRider() ? 1.0D : original;
    }

    @ModifyConstant(method = "clientTick", constant = @Constant(doubleValue = 3.0D), require = 1, allow = 1)
    private static double dimensionworks$keepFastParcelAttached(double original) {
        return dimensionworks$isPackedRider() ? Double.POSITIVE_INFINITY : original;
    }

    @ModifyConstant(method = "clientTick", constant = @Constant(doubleValue = -1.0D), require = 1, allow = 1)
    private static double dimensionworks$keepParcelAttachedOnDescent(double original) {
        return dimensionworks$isPackedRider() ? Double.NEGATIVE_INFINITY : original;
    }
}
