package dev.szx.dimensionworks.transfer.returnpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ReturnPointSavedDataTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.parse("minecraft:overworld");
    @Test
    void roundTripsAllReturnPoints() {
        ReturnPointSavedData source = new ReturnPointSavedData();
        UUID player = UUID.randomUUID();
        ReturnPoint point =
                new ReturnPoint(OVERWORLD, 1.0D, 2.0D, 3.0D, 4.0F, 5.0F);
        source.put(player, point);

        ReturnPointSavedData loaded = ReturnPointSavedData.load(source.save(new CompoundTag(), net.minecraft.core.RegistryAccess.EMPTY));

        assertEquals(point, loaded.get(player).orElseThrow());
    }

    @Test
    void removingPointMakesItUnavailable() {
        ReturnPointSavedData data = new ReturnPointSavedData();
        UUID player = UUID.randomUUID();
        data.put(player, new ReturnPoint(OVERWORLD, 1.0D, 2.0D, 3.0D, 4.0F, 5.0F));

        data.remove(player);

        assertFalse(data.get(player).isPresent());
    }
    @Test void rejectsDuplicateMissingAndNonFiniteReturnPoints() {
        var source = new ReturnPointSavedData();
        source.put(new UUID(0, 1), new ReturnPoint(OVERWORLD, 1, 2, 3, 4, 5));
        var duplicate = source.save(new CompoundTag(), net.minecraft.core.RegistryAccess.EMPTY);
        var list = duplicate.getList("ReturnPoints", 10); list.add(list.getCompound(0).copy());
        assertThrows(IllegalArgumentException.class, () -> ReturnPointSavedData.load(duplicate));
        assertThrows(IllegalArgumentException.class, () -> ReturnPointSavedData.load(new CompoundTag()));
        var invalid = source.save(new CompoundTag(), net.minecraft.core.RegistryAccess.EMPTY);
        invalid.getList("ReturnPoints", 10).getCompound(0).getCompound("ReturnPoint").putDouble("X", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> ReturnPointSavedData.load(invalid));
    }
}
