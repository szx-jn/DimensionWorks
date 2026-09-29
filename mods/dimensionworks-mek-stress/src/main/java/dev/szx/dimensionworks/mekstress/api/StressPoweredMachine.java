package dev.szx.dimensionworks.mekstress.api;

import dev.szx.dimensionworks.mekstress.core.StressPowerState;
import dev.szx.dimensionworks.mekstress.core.StressEnergyBuffer;
import org.jetbrains.annotations.Nullable;

/** Duck interface installed on every Mekanism tile. */
public interface StressPoweredMachine {

    @Nullable
    StressEnergyBuffer dimensionworks$stressEnergyBuffer();

    void dimensionworks$rechargeFromStress(long gameTick);

    int dimensionworks$requiredRpm();

    StressPowerState dimensionworks$stressPowerState();

    void dimensionworks$registerStressSource(StressSupplySource source, long gameTick);

    double dimensionworks$speedMultiplier(long gameTick);

    int dimensionworks$batches(long gameTick);

    boolean dimensionworks$disabledEnergyInfrastructure();
}
