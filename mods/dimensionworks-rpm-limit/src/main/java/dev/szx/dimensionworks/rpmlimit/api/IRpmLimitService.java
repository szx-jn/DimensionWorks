package dev.szx.dimensionworks.rpmlimit.api;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface IRpmLimitService {
    float clamp(KineticBlockEntity kinetic, float requestedSpeed);

    UUID owner(KineticBlockEntity kinetic);

    int effectiveLimit(UUID owner);

    void setLimit(ServerPlayer player, int rpm);

    void setOwner(KineticBlockEntity kinetic, UUID owner);
}
