package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalInsertionMathTest {

    @Test
    void fillsAnEmptySlotUpToItsLimit() {
        assertEquals(20, InternalInsertionMath.intoEmptySlot(20, 64));
        assertEquals(4, InternalInsertionMath.intoEmptySlot(20, 4));
        assertEquals(0, InternalInsertionMath.intoEmptySlot(-1, 4));
    }

    @Test
    void fillsOnlyTheRemainingRoomInAnExistingSlot() {
        assertEquals(12, InternalInsertionMath.intoExistingSlot(52, 20, 64));
        assertEquals(0, InternalInsertionMath.intoExistingSlot(64, 20, 64));
        assertEquals(20, InternalInsertionMath.intoExistingSlot(0, 20, 64));
    }
}
