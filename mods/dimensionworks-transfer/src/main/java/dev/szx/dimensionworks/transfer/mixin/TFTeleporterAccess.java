package dev.szx.dimensionworks.transfer.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=twilightforest.world.TFTeleporter.class,remap=false)
public interface TFTeleporterAccess {
    @Invoker("moveToSafeCoords")
    static Vec3 dimensionworks$moveToSafeCoords(ServerLevel level,Entity entity,BlockPos pos){throw new AssertionError("Mixin not applied");}
}
