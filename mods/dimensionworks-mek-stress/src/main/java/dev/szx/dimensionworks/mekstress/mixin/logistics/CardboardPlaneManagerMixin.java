package dev.szx.dimensionworks.mekstress.mixin.logistics;

import com.kreidev.cmpackagecouriers.CourierTarget;
import com.kreidev.cmpackagecouriers.plane.CardboardPlaneManager;
import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Restricts the fixed Package Couriers version through its actual destination API. */
@Mixin(value = CardboardPlaneManager.class, remap = false)
public abstract class CardboardPlaneManagerMixin {
    @Redirect(method = "addPlane(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFLnet/minecraft/world/item/ItemStack;Z)Z",
        at = @At(value = "INVOKE", target = "Lcom/kreidev/cmpackagecouriers/CourierTarget;getActiveTarget(Ljava/lang/String;Z)Lcom/kreidev/cmpackagecouriers/CourierTarget;"),
        remap = false)
    private static CourierTarget dimensionworks$restrictPlaneTarget(String address, boolean playerTarget,
        Level source, Vec3 position, float yaw, float speed, ItemStack parcel, boolean unpack) {
        CourierTarget target = CourierTarget.getActiveTarget(address, playerTarget);
        if (target == null) return null;
        return DimensionTransportPolicy.standardTransportAllowed(source.dimension().location().toString(),
            target.getDim().location().toString()) ? target : null;
    }
}
