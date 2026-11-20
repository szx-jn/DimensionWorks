package dev.szx.dimensionworks.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import dev.szx.dimensionworks.transfer.territory.TerritorySavedData;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.tutorial.TutorialSavedData;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

/** Reads the sealed source snapshot without starting Minecraft or writing the source files. */
class PersistedTransferSnapshotTest {
    @Test void originalTerritoriesReturnsAndTutorialStatesLoadWithoutDiscardingRecords() throws Exception {
        String directory = System.getProperty("dimensionworks.transferSnapshot");
        assumeTrue(directory != null, "Requires an explicitly supplied sealed migration snapshot");
        var source = Path.of(directory).resolve("world/data");
        var plots = NbtIo.readCompressed(source.resolve(TerritorySavedData.DATA_NAME + ".dat"), NbtAccounter.unlimitedHeap()).getCompound("data");
        var territories = TerritorySavedData.load(plots);
        var saved = territories.save(new net.minecraft.nbt.CompoundTag(), RegistryAccess.EMPTY);
        assertEquals(plots.getInt("NextSpiralIndex"), saved.getInt("NextSpiralIndex"));
        assertEquals(plots.getList("Territories", 10).size(), saved.getList("Territories", 10).size());
        for (var entry : plots.getList("Territories", 10)) {
            var row = (net.minecraft.nbt.CompoundTag) entry; var id = UUID.fromString(row.getString("Player"));
            var plot = territories.get(id);
            assertNotNull(plot); assertEquals(row.getInt("SpiralIndex"), plot.spiralIndex());
            assertEquals(row.getInt("GridX"), plot.gridX()); assertEquals(row.getInt("GridZ"), plot.gridZ());
            assertEquals(row.getBoolean("Completed"), territories.isComplete(id));
            assertEquals(row.getBoolean("AlexCavesUnlocked"), territories.alexCavesUnlocked(id));
        }
        var returns = NbtIo.readCompressed(source.resolve(ReturnPointSavedData.DATA_NAME + ".dat"), NbtAccounter.unlimitedHeap()).getCompound("data");
        var points = ReturnPointSavedData.load(returns);
        assertEquals(returns.getList("ReturnPoints", 10).size(), points.save(new net.minecraft.nbt.CompoundTag(), RegistryAccess.EMPTY).getList("ReturnPoints", 10).size());
        for (var entry : returns.getList("ReturnPoints", 10)) {
            var row = (net.minecraft.nbt.CompoundTag) entry;
            assertEquals(ReturnPoint.load(row.getCompound("ReturnPoint")), points.get(UUID.fromString(row.getString("Player"))).orElseThrow());
        }
        var tutorial = NbtIo.readCompressed(source.resolve("dw_dispatch_tutorial.dat"), NbtAccounter.unlimitedHeap()).getCompound("data");
        var state = TutorialSavedData.load(tutorial);
        assertEquals(tutorial, state.save(new net.minecraft.nbt.CompoundTag(), RegistryAccess.EMPTY));
    }
}
