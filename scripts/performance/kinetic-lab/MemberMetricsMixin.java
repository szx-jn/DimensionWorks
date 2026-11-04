package dev.szx.dimensionworks.rpmlimit.mixin.lab;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.lab.LabMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=KineticBlockEntity.class,remap=false)
public class MemberMetricsMixin {
 @Inject(method="updateFromNetwork",at=@At("HEAD"),remap=false) private void update(float c,float s,int n,CallbackInfo ci) { if(LabMetrics.active) LabMetrics.members++; }
}
