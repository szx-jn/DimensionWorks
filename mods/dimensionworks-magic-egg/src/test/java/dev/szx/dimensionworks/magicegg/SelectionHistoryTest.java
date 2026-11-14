package dev.szx.dimensionworks.magicegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class SelectionHistoryTest {
    @Test
    void savedSelectionSurvivesNbtRoundTripAndPlayerReplacement() throws Exception {
        CompoundTag original = new CompoundTag();
        SelectionHistory.save(original, List.of("minecraft:speed", "minecraft:poison"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(original, output);
        CompoundTag reloaded = NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray()));
        CompoundTag replacement = new CompoundTag();
        CompoundTag otherMod = new CompoundTag();
        otherMod.putBoolean("other_mod_state", true);
        replacement.put(Player.PERSISTED_NBT_TAG, otherMod);
        SelectionHistory.copy(reloaded, replacement);
        assertEquals(List.of("minecraft:speed", "minecraft:poison"), SelectionHistory.read(replacement));
        assertTrue(replacement.getCompound(Player.PERSISTED_NBT_TAG).getBoolean("other_mod_state"));
        assertTrue(SelectionHistory.exists(replacement));
    }

    @Test
    void emptySelectionIsDistinguishedFromFirstUseAndReplacesPreviousChoice() {
        CompoundTag data = new CompoundTag();
        assertFalse(SelectionHistory.exists(data));
        SelectionHistory.save(data, List.of("minecraft:speed"));
        SelectionHistory.save(data, List.of());
        assertTrue(SelectionHistory.exists(data));
        assertEquals(List.of(), SelectionHistory.read(data));
        CompoundTag replacement = new CompoundTag();
        SelectionHistory.copy(data, replacement);
        assertTrue(SelectionHistory.exists(replacement));
        assertEquals(List.of(), SelectionHistory.read(replacement));
    }
}
