package dev.szx.dimensionworks.transfer.worldgen;

import com.mojang.serialization.Codec;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WDTransfer.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> VOID =
            CHUNK_GENERATORS.register("void", () -> VoidChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }
}
