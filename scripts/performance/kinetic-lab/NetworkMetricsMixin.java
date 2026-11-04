package dev.szx.dimensionworks.rpmlimit.mixin.lab;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.lab.LabMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=KineticNetwork.class,remap=false)
public class NetworkMetricsMixin {
 @Inject(method="sync",at=@At("HEAD"),remap=false) private void sync(CallbackInfo ci) { if(LabMetrics.active) LabMetrics.sync++; }
 @Inject(method="getActualStressOf",at=@At("HEAD"),remap=false) private void stress(KineticBlockEntity be, CallbackInfoReturnable<Float> ci) { if(LabMetrics.active) LabMetrics.stress++; }
 @Inject(method="getActualCapacityOf",at=@At("HEAD"),remap=false) private void capacity(KineticBlockEntity be, CallbackInfoReturnable<Float> ci) { if(LabMetrics.active) LabMetrics.capacity++; }
}
