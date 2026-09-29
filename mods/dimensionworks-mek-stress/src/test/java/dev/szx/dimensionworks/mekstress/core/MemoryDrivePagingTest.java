package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryDrivePagingTest {
    @Test
    void usesFourByFivePages() {
        assertEquals(4, MemoryDrivePaging.pageCount(64));
        assertEquals(2, MemoryDrivePaging.pageCount(21));
        assertEquals(1, MemoryDrivePaging.pageCount(0));
    }

    @Test
    void computesPageOffsets() {
        assertEquals(0, MemoryDrivePaging.pageOffset(0));
        assertEquals(20, MemoryDrivePaging.pageOffset(1));
        assertEquals(60, MemoryDrivePaging.pageOffset(3));
    }

    @Test
    void laysOutSlotsInsideFourColumns() {
        assertEquals(0, MemoryDrivePaging.slotX(0));
        assertEquals(18, MemoryDrivePaging.slotX(1));
        assertEquals(54, MemoryDrivePaging.slotX(3));
        assertEquals(0, MemoryDrivePaging.slotX(4));
        assertEquals(0, MemoryDrivePaging.slotY(0));
        assertEquals(18, MemoryDrivePaging.slotY(4));
        assertEquals(72, MemoryDrivePaging.slotY(19));
    }
}
