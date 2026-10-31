package dev.szx.dimensionworks.mekstress.mixin.create;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.szx.dimensionworks.mekstress.reactor.ReactorPowerBridge;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An ordinary Create shaft directly behind a reactor stabilizer acts as a
 * 256 RPM generator. No additional block or BE replacement is required.
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class ReactorShaftMixin {
    @Unique private boolean dimensionworks$reactorOutput;

    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$reactorRpm(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        if (ReactorPowerBridge.outputFor(shaft) != null) {
            cir.setReturnValue(ReactorPowerBridge.OUTPUT_RPM);
        }
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$reactorCapacity(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        ReactorPowerBridge.Output output = ReactorPowerBridge.outputFor(shaft);
        if (output != null) {
            cir.setReturnValue(output.stressPerRpm());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$syncReactorSource(CallbackInfo ci) {
        KineticBlockEntity shaft = (KineticBlockEntity) (Object) this;
        if (shaft.getLevel() == null || shaft.getLevel().isClientSide
            || !ShaftBlock.isShaft(shaft.getBlockState())) {
            return;
        }

        ReactorPowerBridge.Output output = ReactorPowerBridge.outputFor(shaft);
        if (output == null) {
            dimensionworks$detachReactorSource(shaft);
            return;
        }

        if (shaft.getTheoreticalSpeed() == 0 && !shaft.hasSource()) {
            // Create's regular propagation handles the rest of the shaft network.
            shaft.detachKinetics();
            shaft.setSpeed(ReactorPowerBridge.OUTPUT_RPM);
            shaft.setNetwork(shaft.getBlockPos().asLong());
            shaft.attachKinetics();
        }

        if (shaft.getTheoreticalSpeed() != ReactorPowerBridge.OUTPUT_RPM) {
            // Do not overtake an already rotating, incompatible Create generator.
            dimensionworks$detachReactorSource(shaft);
            return;
        }

        KineticNetwork network = shaft.getOrCreateNetwork();
        if (network == null) {
            return;
        }

        float capacity = output.stressPerRpm();
        Float previous = network.sources.put(shaft, capacity);
        if (previous == null) {
            KineticNetworkOptimizer.invalidate(network);
            network.updateNetwork();
        } else if (Float.compare(previous, capacity) != 0) {
            // Create 6 + RPM Limit incremental capacity tracking.
            KineticNetworkOptimizer.updateCapacityContribution(network, shaft);
            network.updateCapacity();
        }
        dimensionworks$reactorOutput = true;
    }

    @Unique
    private void dimensionworks$detachReactorSource(KineticBlockEntity shaft) {
        if (!dimensionworks$reactorOutput) {
            return;
        }
        dimensionworks$reactorOutput = false;

        KineticNetwork network = shaft.hasNetwork() ? shaft.getOrCreateNetwork() : null;
        if (network != null) {
            network.sources.remove(shaft);
            KineticNetworkOptimizer.invalidate(network);
        }

        if (!shaft.hasSource()) {
            shaft.detachKinetics();
            shaft.setSpeed(0.0F);
            shaft.setNetwork(null);
            shaft.attachKinetics();
        } else if (network != null) {
            // The shaft was taken over by a separate Create source: only remove
            // the reactor's capacity, not the other source's rotating network.
            network.updateNetwork();
        }
    }
}
