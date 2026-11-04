package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.memory.IVirtualKineticMember;
import dev.szx.dimensionworks.mekstress.memory.MemoryGridService;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import java.util.Iterator;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {
    @Shadow
    public Map<KineticBlockEntity, Float> members;

    @Shadow
    private float unloadedStress;

    @Inject(method = {"updateNetwork", "updateStress", "updateCapacity"}, at = @At("RETURN"), remap = false)
    private void dimensionworks$invalidateMemoryAllocations(CallbackInfo ci) {
        MemoryGridService.invalidateForKineticMutation((KineticNetwork) (Object) this);
    }

    @Inject(method = {"updateStressFor", "updateCapacityFor"}, at = @At("RETURN"), remap = false)
    private void dimensionworks$invalidateMemoryInputs(KineticBlockEntity member, float contribution,
                                                        CallbackInfo ci) {
        MemoryGridService.invalidateForKineticMutation((KineticNetwork) (Object) this);
    }

    @Inject(method = {"add", "remove"}, at = @At("RETURN"), remap = false)
    private void dimensionworks$invalidateMemoryTopology(KineticBlockEntity member, CallbackInfo ci) {
        MemoryGridService.invalidateForKineticMember(member);
    }

    @Inject(method = "calculateStress", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$calculateVirtualMachineStress(CallbackInfoReturnable<Float> cir) {
        KineticNetwork network = (KineticNetwork) (Object) this;
        Float cached = KineticNetworkOptimizer.cachedStress(network);
        if (cached != null) {
            cir.setReturnValue(cached);
            return;
        }

        KineticNetworkOptimizer.beginStressRebuild(network);
        float presentStress = 0.0F;
        Iterator<KineticBlockEntity> iterator = members.keySet().iterator();
        while (iterator.hasNext()) {
            KineticBlockEntity member = iterator.next();
            if (member instanceof IVirtualKineticMember virtualMember) {
                if (!virtualMember.dimensionworks$isValid()) {
                    iterator.remove();
                    KineticNetworkOptimizer.onMemberPruned(network, member);
                    continue;
                }
            } else if (member.getLevel() == null
                || member.getLevel().getBlockEntity(member.getBlockPos()) != member) {
                iterator.remove();
                KineticNetworkOptimizer.onMemberPruned(network, member);
                continue;
            }

            float contribution = network.getActualStressOf(member);
            KineticNetworkOptimizer.recordStressContribution(network, member, contribution);
            presentStress += contribution;
        }
        float totalStress = presentStress + unloadedStress;
        KineticNetworkOptimizer.finishStressRebuild(network, totalStress);
        cir.setReturnValue(totalStress);
    }
}
