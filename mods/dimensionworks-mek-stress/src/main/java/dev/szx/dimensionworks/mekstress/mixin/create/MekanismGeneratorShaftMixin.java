package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.integration.mekgenerators.GeneratorPowerBridge;
import dev.szx.dimensionworks.mekstress.reactor.ReactorPowerBridge;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class MekanismGeneratorShaftMixin implements dev.szx.dimensionworks.mekstress.api.IMechanicalSourceShaft {
    @Unique private boolean dimensionworks$mekGeneratorSource;
    @Shadow protected float lastCapacityProvided;

    @Unique private static float dimensionworks$capacity(KineticBlockEntity shaft) {
        var reactor = ReactorPowerBridge.outputFor(shaft);
        return reactor == null ? GeneratorPowerBridge.stressPerRpm(shaft) : reactor.stressPerRpm();
    }

    @Unique private static float dimensionworks$rpm(KineticBlockEntity shaft) {
        return ReactorPowerBridge.outputFor(shaft) == null
            ? GeneratorPowerBridge.outputRpm() : ReactorPowerBridge.OUTPUT_RPM;
    }

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$generatorSpeed(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        if (dimensionworks$capacity(shaft) > 0 && cir.getReturnValueF() == 0)
            cir.setReturnValue(dimensionworks$rpm(shaft));
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$generatorCapacity(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        float capacity = dimensionworks$capacity(shaft);
        if (capacity > 0) {
            lastCapacityProvided = cir.getReturnValueF() + capacity;
            cir.setReturnValue(lastCapacityProvided);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$syncGeneratorSource(CallbackInfo ci) {
        dimensionworks$refreshMechanicalSource();
    }

    @Override public void dimensionworks$refreshMechanicalSource() {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        if (shaft.getLevel() == null || shaft.getLevel().isClientSide) return;
        float capacity = dimensionworks$capacity(shaft);
        float rpm = dimensionworks$rpm(shaft);
        if (capacity <= 0 || (shaft.getTheoreticalSpeed() != 0 && shaft.getTheoreticalSpeed() != rpm)) {
            dimensionworks$detach(shaft);
            return;
        }
        if (shaft.getTheoreticalSpeed() == 0 && !shaft.hasSource()) {
            shaft.detachKinetics();
            shaft.setSpeed(rpm);
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
        if (!dimensionworks$mekGeneratorSource) return;
        dimensionworks$mekGeneratorSource = false;
        lastCapacityProvided = 0;
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
