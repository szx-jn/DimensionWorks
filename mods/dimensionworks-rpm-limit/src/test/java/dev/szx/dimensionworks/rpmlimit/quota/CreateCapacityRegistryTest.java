package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateCapacityRegistryTest {

    private final UUID ownerA = UUID.fromString("1cf45a7f-b37f-49ac-a843-a301ba41f95a");
    private final UUID ownerB = UUID.fromString("7a171027-d8a6-4c53-a438-9d744f803c84");

    @Test
    void scalesOwnerAggregateTo4096WithoutExceedingIt() {
        Map<UUID, Double> raw = Map.of(ownerA, 3000.0, ownerB, 5000.0);

        assertEquals(3000.0, CreateCapacityRegistry.scaleBucket(raw, ownerA, 4096.0));
        assertEquals(4096.0, CreateCapacityRegistry.scaleBucket(raw, ownerB, 4096.0));
    }

    @Test
    void zeroAndNegativeTargetsNeverRaiseCapacity() {
        Map<UUID, Double> raw = Map.of(ownerA, 5000.0);

        assertEquals(0.0, CreateCapacityRegistry.scaleBucket(raw, ownerA, 0.0));
        assertEquals(0.0, CreateCapacityRegistry.scaleBucket(raw, ownerA, -1.0));
        assertEquals(0.0, CreateCapacityRegistry.scaleBucket(Map.of(), ownerA, 4096.0));
    }

    @Test
    void ignoresNonFiniteSamples() {
        Map<UUID, Double> raw = Map.of(
            ownerA, 1000.0,
            ownerB, Double.NaN
        );

        assertEquals(1000.0, CreateCapacityRegistry.scaleBucket(raw, ownerA, 4096.0));
    }

    @Test
    void repairedCreateCapacityReturnsNaturalValue() {
        Map<UUID, Double> raw = Map.of(ownerA, 9000.0);

        assertEquals(9000.0F, CreateCapacityRegistry.applyOwnerQuota(
            raw, ownerA, 9000.0F, 0b0000010, 4096.0, 0.0));
        assertEquals(4096.0F, CreateCapacityRegistry.applyOwnerQuota(
            raw, ownerA, 9000.0F, 0, 4096.0, 0.0));
        assertEquals(4096.0F, CreateCapacityRegistry.applyOwnerQuota(
            raw, ownerA, 9000.0F, 0, 5000.0, 0.0));
        assertEquals(1096.0F, CreateCapacityRegistry.applyOwnerQuota(
            raw, ownerA, 9000.0F, 0, 4096.0, 3000.0));
    }
}
