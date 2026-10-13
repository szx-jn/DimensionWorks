package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FixedIntervalTokenBucketTest {

    private final UUID owner = UUID.fromString("9f3f75c4-55cb-4f7e-a72c-40bb63786145");

    @Test
    void refillsOnlyTheCurrentTickAllowanceAndDoesNotAccumulate() {
        FixedIntervalTokenBucket bucket = new FixedIntervalTokenBucket();

        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 0));
        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 1));
        assertFalse(bucket.tryTake(
            owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.5, 8.0, 1));

        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 100));
        assertFalse(bucket.tryTake(
            owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.5, 8.0, 100));
    }

    @Test
    void zeroRateRejectsPositiveRequestsAndNonPositiveRequestsPass() {
        FixedIntervalTokenBucket bucket = new FixedIntervalTokenBucket();

        assertFalse(bucket.tryTake(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, 1.0, 0.0, 0));
        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, 0.0, 0.0, 0));
        assertFalse(bucket.tryTake(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, -1.0, 8.0, 0));
    }

    @Test
    void isolatesPlayersAndQuotaTypes() {
        FixedIntervalTokenBucket bucket = new FixedIntervalTokenBucket();
        UUID other = UUID.fromString("66728a75-7c8e-4f8e-a779-f4d713602f17");

        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.CROSS_PROCESS, 0.8, 16.0, 0));
        assertTrue(bucket.tryTake(
            other, GearHeartQuotaType.CROSS_PROCESS, 0.8, 16.0, 0));
        assertTrue(bucket.tryTake(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, 0.8, 16.0, 0));
        assertFalse(bucket.tryTake(
            owner, GearHeartQuotaType.CROSS_PROCESS, 0.1, 16.0, 0));
    }
}
