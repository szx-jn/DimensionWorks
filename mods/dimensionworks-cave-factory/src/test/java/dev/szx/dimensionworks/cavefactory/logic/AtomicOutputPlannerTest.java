package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicOutputPlannerTest {

    @Test
    void requiresEveryOutputToFit() {
        assertTrue(AtomicOutputPlanner.canFit(16, 16, 64));
        assertTrue(AtomicOutputPlanner.allFit(32, 64));
        assertFalse(AtomicOutputPlanner.allFit(64, 32));
    }

    @Test
    void exactCapacityFits() {
        assertTrue(AtomicOutputPlanner.canFit(64, 0, 64));
        assertFalse(AtomicOutputPlanner.canFit(65, 0, 64));
    }

    @Test
    void clampsNegativeValues() {
        assertTrue(AtomicOutputPlanner.canFit(-5, 0, 64));
    }

    @Test
    void outputPlannerHonorsRealStackAndSlotCapacity() {
        ItemStackHandler oneSlot = new ItemStackHandler(1);
        assertFalse(AtomicOutputPlanner.canFitOutputs(oneSlot, List.of(new ItemStack(Items.STONE, 65))));

        ItemStackHandler twoSlots = new ItemStackHandler(2);
        assertTrue(AtomicOutputPlanner.canFitOutputs(twoSlots, List.of(new ItemStack(Items.STONE, 65))));
    }
}
