package dev.szx.dimensionworks.rpmlimit.api;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

public interface IOverspeedService {
    boolean isEnabled();

    boolean isStressScalingEnabled();

    boolean isWhitelisted(KineticBlockEntity kinetic);

    double multiplier(double rpm, double saturationRpm);

    int batches(double multiplier, long tick);
}
