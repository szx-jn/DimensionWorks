package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.szx.dimensionworks.mekstress.integration.mekgenerators.GeneratorPowerBridge;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class MekanismGeneratorShaftMixin {
    @Unique private boolean dimensionworks$mekGeneratorSource;
    @Shadow protected float lastCapacityProvided;

    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$generatorSpeed(CallbackInfoReturnable<Float> cir) {
        if (GeneratorPowerBridge.stressPerRpm((KineticBlockEntity) (Object) this) > 0)
            cir.setReturnValue(GeneratorPowerBridge.outputRpm());
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$generatorCapacity(CallbackInfoReturnable<Float> cir) {
        float capacity = GeneratorPowerBridge.stressPerRpm((KineticBlockEntity) (Object) this);
        if (capacity > 0) {
            lastCapacityProvided = capacity;
            cir.setReturnValue(capacity);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$syncGeneratorSource(CallbackInfo ci) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        if (shaft.getLevel() == null || shaft.getLevel().isClientSide || !ShaftBlock.isShaft(shaft.getBlockState())) return;
        float capacity = GeneratorPowerBridge.stressPerRpm(shaft);
        if (capacity <= 0 || (shaft.getTheoreticalSpeed() != 0 && shaft.getTheoreticalSpeed() != GeneratorPowerBridge.outputRpm())) {
            dimensionworks$detach(shaft);
            return;
        }
        if (shaft.getTheoreticalSpeed() == 0 && !shaft.hasSource()) {
            shaft.detachKinetics();
            shaft.setSpeed(GeneratorPowerBridge.outputRpm());
            shaft.setNetwork(shaft.getBlockPos().asLong());
            shaft.attachKinetics();
        }
        KineticNetwork network = shaft.getOrCreateNetwork();
        if (network == null) return;
        lastCapacityProvided = capacity;
        Float previous = network.sources.put(shaft, capacity);
        if (previous == null) {
            KineticNetworkOptimizer.invalidate(network);
            network.updateNetwork();
        } else if (Float.compare(previous, capacity) != 0) {
            KineticNetworkOptimizer.updateCapacityContribution(network, shaft);
            network.updateCapacity();
        }
        dimensionworks$mekGeneratorSource = true;
    }

    @Unique private void dimensionworks$detach(KineticBlockEntity shaft) {
        KineticNetwork network = shaft.hasNetwork() ? shaft.getOrCreateNetwork() : null;
        if (!dimensionworks$mekGeneratorSource && (network == null || !network.sources.containsKey(shaft))) return;
        dimensionworks$mekGeneratorSource = false;
        if (network != null) {
            network.sources.remove(shaft);
            KineticNetworkOptimizer.invalidate(network);
        }
        if (!shaft.hasSource()) {
            shaft.detachKinetics();
            shaft.setSpeed(0);
            shaft.setNetwork(null);
            shaft.attachKinetics();
        } else if (network != null) network.updateNetwork();
    }
}
