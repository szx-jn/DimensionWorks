package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureLayoutTest {

    @Test
    void recognizesCenterAndFaceCenters() {
        assertTrue(StructureLayout.isCenter(0, 0, 0));
        assertTrue(StructureLayout.isFaceCenter(1, 0, 0));
        assertTrue(StructureLayout.isFaceCenter(0, -1, 0));
        assertTrue(StructureLayout.isFaceCenter(0, 0, 1));
        assertFalse(StructureLayout.isFaceCenter(1, 1, 0));
        assertFalse(StructureLayout.isFaceCenter(0, 0, 0));
    }

    @Test
    void includesTwentySixShellPositions() {
        int shell = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!StructureLayout.isCenter(x, y, z)) {
                        shell++;
                    }
                }
            }
        }
        assertTrue(shell == 26);
    }
}
