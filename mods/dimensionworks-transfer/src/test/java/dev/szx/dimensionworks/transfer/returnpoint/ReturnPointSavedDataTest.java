package dev.szx.dimensionworks.transfer.returnpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

        ReturnPointSavedData loaded = ReturnPointSavedData.load(source.save(new CompoundTag()));

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
}
