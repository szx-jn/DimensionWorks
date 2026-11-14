package dev.szx.dimensionworks.transfer.elastic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElasticChargeTest {
    @Test void continuousHoldCapsAtThreeSecondsAndReleasesOnce() {
        var charge = new ElasticCharge();
        charge.input(true);
        for (int i = 0; i < 60; i++) charge.tick(true);
        assertEquals(60, charge.ticks());
        for (int i = 0; i < 200; i++) charge.tick(true);
        assertEquals(60, charge.input(false));
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

    @Test void greaterChargeIncreasesStrengthWithinFixedMaximum() {
        assertEquals(0, ElasticCharge.speed(0));
        assertTrue(ElasticCharge.speed(20) < ElasticCharge.speed(40));
        assertTrue(ElasticCharge.speed(40) < ElasticCharge.speed(60));
        assertEquals(1.65, ElasticCharge.speed(60), 1e-9);
        assertEquals(ElasticCharge.speed(60), ElasticCharge.speed(200));
    }
}
