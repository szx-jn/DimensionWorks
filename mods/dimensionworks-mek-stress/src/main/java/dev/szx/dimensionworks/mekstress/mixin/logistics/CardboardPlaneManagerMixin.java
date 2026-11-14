package dev.szx.dimensionworks.mekstress.mixin.logistics;

import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Restricts Create More: Package Couriers' interdimensional cardboard-plane routes. */
@Pseudo
@Mixin(targets = "com.kreidev.cmpackagecouriers.plane.CardboardPlaneManager", remap = false)
public abstract class CardboardPlaneManagerMixin {
    @Coerce
    @Redirect(method = "addPlane(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFLnet/minecraft/world/item/ItemStack;Z)Z",
        at = @At(value = "INVOKE", target = "Lcom/kreidev/cmpackagecouriers/CourierTarget;getActiveTarget(Ljava/lang/String;Z)Lcom/kreidev/cmpackagecouriers/CourierTarget;"),
        remap = false)
    private static Object dimensionworks$restrictPlaneTarget(String address, boolean playerTarget,
        Level source, Vec3 position, float yaw, float speed, ItemStack parcel, boolean unpack) {
        try {
            Class<?> targetClass = Class.forName("com.kreidev.cmpackagecouriers.CourierTarget");
            Method lookup = targetClass.getMethod("getActiveTarget", String.class, boolean.class);
            Object target = lookup.invoke(null, address, playerTarget);
            if (target == null) return null;
            Object value = targetClass.getMethod("getDim").invoke(target);
            if (!(value instanceof ResourceKey<?> destination)) return null;
            String sourceId = source.dimension().location().toString();
            String destinationId = destination.location().toString();
            return DimensionTransportPolicy.standardTransportAllowed(sourceId, destinationId) ? target : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }
}
