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

/** Loads TF's registered stem or its active datapack definition, never a copied generator. */
public final class TwilightDestination {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
        new ResourceLocation("twilightforest", "twilight_forest"));
    public static final String AUTHORIZATION = "dw_dispatch_twilight_authorized";
    private TwilightDestination() {}

    public static ServerLevel getOrLoad(MinecraftServer server) throws IOException {
        ServerLevel existing = server.getLevel(KEY);
        if (existing != null) return existing;
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(KEY.location());
        if (stem == null) {
            var resource = server.getResourceManager().getResource(
                new ResourceLocation("twilightforest", "dimension/twilight_forest.json"));
            if (resource.isEmpty()) return null;
            try (var reader = resource.get().openAsReader()) {
                stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()),
                    JsonParser.parseReader(reader)).getOrThrow(false,
                        error -> WDTransfer.LOGGER.error("Cannot decode Twilight Forest dimension: {}", error));
            }
        }
        MinecraftServerAccess access = (MinecraftServerAccess) server;
        var worldData = server.getWorldData();
        ServerLevel level = new ServerLevel(server, access.dimensionworks$executor(), access.dimensionworks$storage(),
            new DerivedLevelData(worldData, worldData.overworldData()), KEY, stem,
            new LoggerChunkProgressListener(0), worldData.isDebugWorld(),
            BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()), List.of(), false,
            server.overworld().getRandomSequences());
        server.overworld().getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getWorldBorder().applySettings(server.overworld().getWorldBorder().createSettings());
        access.dimensionworks$levels().put(KEY, level);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }
}
