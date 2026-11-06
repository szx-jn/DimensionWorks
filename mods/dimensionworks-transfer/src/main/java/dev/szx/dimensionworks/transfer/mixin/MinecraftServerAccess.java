package dev.szx.dimensionworks.transfer.mixin;

import java.util.Map;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The same storage and executor used by the server's normal dimension bootstrap. */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccess {
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess dimensionworks$storage();
    @Accessor("executor") Executor dimensionworks$executor();
    @Accessor("levels") Map<ResourceKey<Level>, ServerLevel> dimensionworks$levels();
}
