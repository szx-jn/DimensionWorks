package dev.szx.dimensionworks.rpmlimit.quota;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Resolves kinetic ownership without allowing missing owners to bypass quotas. */
public final class GearHeartOwnerResolver {
    public static final UUID SHARED_OWNER = UUID.nameUUIDFromBytes(
        "dimensionworks:gear_heart:shared".getBytes(StandardCharsets.UTF_8));

    private GearHeartOwnerResolver() {
    }

    public static UUID owner(KineticBlockEntity kinetic) {
        return normalize(RpmLimitManager.owner(kinetic));
    }

    public static UUID normalize(UUID owner) {
        return owner == null ? SHARED_OWNER : owner;
    }
}
