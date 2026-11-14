package dev.szx.dimensionworks.transfer.elastic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElasticChargeTest {
    @Test void continuousHoldCapsAtTwoSecondsAndReleasesOnce() {
        var charge = new ElasticCharge();
        charge.input(true);
        for (int i = 0; i < 39; i++) charge.tick(true);
        assertEquals(39, charge.ticks());
        charge.tick(true);
        assertEquals(40, charge.ticks());
        for (int i = 0; i < 200; i++) charge.tick(true);
        assertEquals(40, charge.input(false));
        assertEquals(0, charge.input(false));
        assertEquals(0, charge.ticks());
    }

    @Test void leavingGroundCancelsAccumulationAndRelease() {
        var charge = new ElasticCharge();
        charge.input(true);
        for (int i = 0; i < 40; i++) charge.tick(true);
        charge.tick(false);
        assertEquals(0, charge.input(false));
    }

    @Test void holdingInAirBeginsFreshWhenLanding() {
        var charge = new ElasticCharge();
        charge.input(true);
        for (int i = 0; i < 80; i++) charge.tick(false);
        for (int i = 0; i < 12; i++) charge.tick(true);
        assertEquals(12, charge.input(false));
    }

    @Test void repeatedPressPacketsDoNotResetOrDuplicateCharge() {
        var charge = new ElasticCharge();
        charge.input(true);
        for (int i = 0; i < 15; i++) { charge.input(true); charge.tick(true); }
        assertEquals(15, charge.input(false));
        for (int i = 0; i < 60; i++) charge.tick(true);
        assertEquals(0, charge.ticks());
    }

    @Test void partialChargeRetainsOriginalStrengthAtEveryTick() {
        assertEquals(0, ElasticCharge.speed(0));
        for (int ticks = 1; ticks < 40; ticks++) {
            assertEquals(0.35 + 3.3625 * ticks / 60, ElasticCharge.speed(ticks),
                "Partial charge strength changed at tick " + ticks);
        }
    }

    @Test void onlyFullChargeReceivesFiveTimesPreviousFullStrength() {
        assertEquals(18.5625, ElasticCharge.speed(40), 1e-9);
        assertEquals(ElasticCharge.speed(40), ElasticCharge.speed(60));
        assertEquals(ElasticCharge.speed(40), ElasticCharge.speed(200));
    }
}
