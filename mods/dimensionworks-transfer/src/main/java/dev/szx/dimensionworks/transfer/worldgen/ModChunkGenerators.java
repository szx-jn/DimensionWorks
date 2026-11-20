package dev.szx.dimensionworks.transfer.worldgen;

import com.mojang.serialization.MapCodec;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WDTransfer.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> VOID =
            CHUNK_GENERATORS.register("void", () -> VoidChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }
}
