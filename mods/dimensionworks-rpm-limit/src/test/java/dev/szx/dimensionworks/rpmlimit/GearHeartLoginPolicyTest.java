package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearHeartLoginPolicyTest {

    @Test
    void emptySlotIsFilledButOccupiedSlotIsLeftAlone() {
        assertTrue(GearHeartState.shouldFillEmptySlot(true));
        assertFalse(GearHeartState.shouldFillEmptySlot(false));
    }

    @Test
    void replacementHeartStartsWithEveryCurseUnrepaired() {
        CompoundTag heart = GearHeartState.unrepairedCursedHeartTag();

        assertFalse(heart.contains("CustomModelData"), "模型编号应使用独立数据组件");
        assertEquals(0, heart.getInt(GearHeartState.REPAIRED_MASK_KEY));
    }
}
