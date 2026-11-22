package dev.szx.dimensionworks.rpmlimit.ccb;

/**
 * Maps the pressure boost of CCB's airtight pump to its RPM-equivalent basin/belt batch ceiling.
 */
public final class CcbGasInjectionBatchCurve {

    public static final int MAX_ROTATION_SPEED_RPM = 10240;
    public static final long MAX_PUMP_BOOST_PA = 400000;
    public static final int MINIMUM_PROCESSING_RPM = 1024;
    public static final long MINIMUM_PUMP_BOOST_PA =
        (MAX_PUMP_BOOST_PA * MINIMUM_PROCESSING_RPM + MAX_ROTATION_SPEED_RPM - 1)
            / MAX_ROTATION_SPEED_RPM;

    private CcbGasInjectionBatchCurve() {}

    public static boolean hasMinimumPumpBoost(long pressureBoostPa) {
        return pressureBoostPa >= MINIMUM_PUMP_BOOST_PA;
    }

    public static int limitBatch(long pressureBoostPa, int plannedBatch) {
        if (plannedBatch <= 0)
            return plannedBatch;
        if (!hasMinimumPumpBoost(pressureBoostPa))
            return 0;
        return Math.min(plannedBatch, batchLimit(pressureBoostPa));
    }

    public static int batchLimit(long pressureBoostPa) {
        if (pressureBoostPa >= boostAt(8192))
            return 64;
        if (pressureBoostPa >= boostAt(6144))
            return 48;
        if (pressureBoostPa >= boostAt(4096))
            return 32;
        if (pressureBoostPa >= boostAt(2048))
            return 24;
        if (pressureBoostPa >= boostAt(1024))
            return 16;
        if (pressureBoostPa >= boostAt(512))
            return 12;
        if (pressureBoostPa >= boostAt(256))
            return 8;
        return 1;
    }

    private static long boostAt(int rpm) {
        return (MAX_PUMP_BOOST_PA * rpm + MAX_ROTATION_SPEED_RPM - 1) / MAX_ROTATION_SPEED_RPM;
    }
}
