package dev.szx.dimensionworks.transfer.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class TerritorySavedDataTest {
    private CompoundTag fixture() {
        var data = new TerritorySavedData();
        for (int i = 0; i < 5; i++) {
            var id = new UUID(0, i + 1); data.getOrAssign(id);
            if (i == 1) data.markComplete(id);
            if (i == 2) data.unlockAlexCaves(id);
        }
        return data.save(new CompoundTag(), RegistryAccess.EMPTY);
    }
    @Test void preservesPlotCoordinatesFlagsAndNextAllocation() {
        var loaded = TerritorySavedData.load(fixture());
        for (int i = 0; i < 5; i++) {
            var id = new UUID(0, i + 1); var grid = SpiralAllocator.indexToGrid(i);
            assertEquals(new Territory(i, grid[0], grid[1]), loaded.get(id));
            assertEquals(i == 1, loaded.isComplete(id)); assertEquals(i == 2, loaded.alexCavesUnlocked(id));
        }
        assertEquals(5, loaded.getOrAssign(new UUID(0, 6)).spiralIndex());
    }
    @Test void rejectsMissingListInsteadOfCreatingAnEmptyWorld() {
        var tag = fixture(); tag.remove("Territories");
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(tag));
    }
    @Test void rejectsDuplicateOwnerAndIndexBeforeAllocatingAnotherPlot() {
        var tag = fixture(); var list = tag.getList("Territories", 10);
        list.add(list.getCompound(0).copy());
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(tag));
        var duplicateOwner = fixture(); var owners = duplicateOwner.getList("Territories", 10);
        owners.getCompound(1).putString("Player", owners.getCompound(0).getString("Player"));
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(duplicateOwner));
    }
    @Test void rejectsGridDriftWrongFlagsAndAllocationBehindExistingPlot() {
        var tag = fixture(); tag.getList("Territories", 10).getCompound(0).putInt("GridX", 77);
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(tag));
        var flags = fixture(); flags.getList("Territories", 10).getCompound(0).putString("Completed", "true");
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(flags));
        var allocation = fixture(); allocation.putInt("NextSpiralIndex", 1);
        assertThrows(IllegalArgumentException.class, () -> TerritorySavedData.load(allocation));
    }
}
