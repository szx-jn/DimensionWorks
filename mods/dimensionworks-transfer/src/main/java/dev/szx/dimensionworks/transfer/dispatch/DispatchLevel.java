package dev.szx.dimensionworks.transfer.dispatch;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.mixin.MinecraftServerAccess;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;

/** Loads a destination's owning datapack definition, without copying third-party generators. */
public final class DispatchLevel {
    private DispatchLevel() {}

    public static ServerLevel getOrLoad(MinecraftServer server, ResourceKey<Level> key) throws IOException {
        ServerLevel existing = server.getLevel(key);
        if (existing != null) return existing;
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(key.location());
        if (stem == null) {
            var resource = server.getResourceManager().getResource(new ResourceLocation(
                key.location().getNamespace(), "dimension/" + key.location().getPath() + ".json"));
            if (resource.isEmpty()) return null;
            try (var reader = resource.get().openAsReader()) {
                stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()),
                    JsonParser.parseReader(reader)).getOrThrow(false,
                        error -> WDTransfer.LOGGER.error("Cannot decode destination dimension {}: {}", key.location(), error));
            }
        }
        MinecraftServerAccess access = (MinecraftServerAccess) server;
        var worldData = server.getWorldData();
        ServerLevel level = new ServerLevel(server, access.dimensionworks$executor(), access.dimensionworks$storage(),
            new DerivedLevelData(worldData, worldData.overworldData()), key, stem,
            new LoggerChunkProgressListener(0), worldData.isDebugWorld(),
            BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()), List.of(), false,
            server.overworld().getRandomSequences());
        server.overworld().getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getWorldBorder().applySettings(server.overworld().getWorldBorder().createSettings());
        access.dimensionworks$levels().put(key, level);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }
}
