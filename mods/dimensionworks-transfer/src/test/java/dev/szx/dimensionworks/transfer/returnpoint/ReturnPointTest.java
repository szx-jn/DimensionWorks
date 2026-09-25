package dev.szx.dimensionworks.transfer.returnpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ReturnPointTest {
    @Test
    void roundTripsThroughNbt() {
        ReturnPoint expected = new ReturnPoint(
                ResourceLocation.parse("minecraft:overworld"), 12.5D, 64.0D, -9.25D, 90.0F, -15.0F);

        assertEquals(expected, ReturnPoint.load(expected.save()));
    }

    @Test
    void rejectsMissingDimension() {
        CompoundTag tag = new CompoundTag();

        assertThrows(IllegalArgumentException.class, () -> ReturnPoint.load(tag));
    }

    @Test
    void rejectsInvalidCoordinates() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", "minecraft:overworld");
        tag.putString("X", "not-a-number");

        assertThrows(IllegalArgumentException.class, () -> ReturnPoint.load(tag));
    }
}
