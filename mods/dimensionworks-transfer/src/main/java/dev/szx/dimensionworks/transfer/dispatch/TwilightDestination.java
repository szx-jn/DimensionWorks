package dev.szx.dimensionworks.transfer.dispatch;

import java.io.IOException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class TwilightDestination {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
        new ResourceLocation("twilightforest", "twilight_forest"));
    public static final String AUTHORIZATION = "dw_dispatch_twilight_authorized";

    private TwilightDestination() {}

    public static ServerLevel getOrLoad(MinecraftServer server) throws IOException {
        return DispatchLevel.getOrLoad(server, KEY);
    }
}
