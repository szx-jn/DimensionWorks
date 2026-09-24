package dev.szx.dimensionworks.rpmlimit.mixin;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity; import dev.szx.dimensionworks.rpmlimit.RpmLimitManager; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(KineticBlockEntity.class) public abstract class KineticBlockEntityMixin { @ModifyVariable(method="setSpeed",at=@At("HEAD"),argsOnly=true) private float limit(float speed){return RpmLimitManager.clamp((KineticBlockEntity)(Object)this,speed);} }
