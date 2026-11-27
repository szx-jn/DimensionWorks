package dev.szx.dimensionworks.rpmlimit.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Public service contracts shared with other DimensionWorks modules. */
public final class RpmLimitServices {
    public record Services(
        IRpmLimitService rpm,
        IKineticOptimizerService optimizer,
        IOverspeedService overspeed,
        IQuotaGateService quota,
        ITopologyAdapterRegistry topology
    ) {
        public Services {
            Objects.requireNonNull(rpm, "rpm");
            Objects.requireNonNull(optimizer, "optimizer");
            Objects.requireNonNull(overspeed, "overspeed");
            Objects.requireNonNull(quota, "quota");
            Objects.requireNonNull(topology, "topology");
        }
    }

    private static final AtomicReference<Services> INSTALLED = new AtomicReference<>();

    private RpmLimitServices() {
    }

    public static void install(Services services) {
        Objects.requireNonNull(services, "services");
        if (!INSTALLED.compareAndSet(null, services)) {
            throw new IllegalStateException("RPM Limit services are already installed");
        }
    }

    public static IRpmLimitService rpm() {
        return services().rpm();
    }

    public static IKineticOptimizerService optimizer() {
        return services().optimizer();
    }

    public static IOverspeedService overspeed() {
        return services().overspeed();
    }

    public static IQuotaGateService quota() {
        return services().quota();
    }

    public static ITopologyAdapterRegistry topology() {
        return services().topology();
    }

    private static Services services() {
        Services services = INSTALLED.get();
        if (services == null) {
            throw new IllegalStateException("RPM Limit services were used before initialization");
        }
        return services;
    }
}
