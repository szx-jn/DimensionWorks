package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MemoryNetworkMatrixTest {
    private static final long MAX_IMPORT_SU_PER_TICK = 16_384L;
    private static final int DENSE_MACHINES_PER_CHUNK = 16;

    @Test
    void recordsTheFullCardAndMachineMatrix() throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("cards,ddr,capacity_su,bandwidth_rpm,machines,basic,advanced,elite,ultimate,"
            + "demand_rpm,demand_su_per_tick,su_utilization,bandwidth_utilization,q_stock,q_bandwidth,q,"
            + "production_multiplier,"
            + "import_gearboxes_at_max_rate,loaded_block_entities,dense_chunks_at_16_per_chunk");

        for (MemoryTier tier : MemoryTier.values()) {
            int cards = tier.cardsInFullNetwork();
            long capacity = tier.totalCapacitySu();
            long bandwidth = tier.totalBandwidthRpm();
            for (int machineCount = 10; machineCount <= 150; machineCount += 10) {
                Mix mix = mix(machineCount);
                long demandRpm = mix.demandRpm();
                long demandSu = demandRpm * 8L;
                double suUtilization = utilization(demandSu, capacity);
                double bandwidthUtilization = utilization(demandRpm, bandwidth);
                double qStock = NetworkMath.stockRatio(capacity, demandSu);
                double qBandwidth = NetworkMath.bandwidthQ(bandwidth, demandRpm);
                double q = NetworkMath.finalQ(qStock, qBandwidth);
                double production = NetworkMath.productionRate(1.0D, q, 1.25D);
                long importGearboxes = ceilDiv(demandSu, MAX_IMPORT_SU_PER_TICK);
                int loadedBlockEntities = machineCount + MemoryTier.MAX_DRIVES_PER_NETWORK;
                int denseChunks = ceilDiv(loadedBlockEntities, DENSE_MACHINES_PER_CHUNK);

                rows.add(String.format(Locale.ROOT,
                    "%d,DDR%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%d,%d",
                    cards, tier.index(), capacity, bandwidth, machineCount,
                    mix.basic(), mix.advanced(), mix.elite(), mix.ultimate(),
                    demandRpm, demandSu, suUtilization, bandwidthUtilization, qStock, qBandwidth, q, production,
                    importGearboxes, loadedBlockEntities, denseChunks));
            }
        }

        Path report = Path.of("build", "reports", "dimensionworks-me-memory-matrix.csv");
        Files.createDirectories(report.getParent());
        Files.write(report, rows, StandardCharsets.UTF_8);

        assertEquals(15 * MemoryTier.values().length, rows.size() - 1);
        assertTrue(Files.size(report) > 0L);
    }

    @Test
    void fullDdr5HasTheDesignedTerminalMargin() {
        Mix terminal = new Mix(10, 100, 0, 0);
        assertEquals(245_760L, terminal.demandRpm());
        assertEquals(1_966_080L, terminal.demandSu());
        assertEquals(1.0D, NetworkMath.bandwidthQ(MemoryTier.DDR5.totalBandwidthRpm(), terminal.demandRpm()),
            1.0E-9D);
        assertEquals(1.0D, NetworkMath.stockRatio(MemoryTier.DDR5.totalCapacitySu(), terminal.demandSu()),
            1.0E-9D);
        assertEquals(0.9375D, utilization(terminal.demandRpm(), MemoryTier.DDR5.totalBandwidthRpm()), 1.0E-9D);
        assertEquals(0.9375D, utilization(terminal.demandSu(), MemoryTier.DDR5.totalCapacitySu()), 1.0E-9D);
    }

    private static Mix mix(int machines) {
        int ultimate = Math.max(1, Math.round(machines * 0.10F));
        int elite = Math.max(1, Math.round(machines * 0.15F));
        int advanced = Math.max(1, Math.round(machines * 0.25F));
        int basic = Math.max(1, machines - ultimate - elite - advanced);
        return new Mix(ultimate, elite, advanced, basic);
    }

    private static double utilization(long demand, long supply) {
        if (demand <= 0L) {
            return 0.0D;
        }
        return supply <= 0L ? Double.POSITIVE_INFINITY : (double) demand / (double) supply;
    }

    private static long ceilDiv(long value, long divisor) {
        return (value + divisor - 1L) / divisor;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private record Mix(int ultimate, int elite, int advanced, int basic) {
        private long demandRpm() {
            return (long) ultimate * MachineTier.ULTIMATE.targetRpm()
                + (long) elite * MachineTier.ELITE.targetRpm()
                + (long) advanced * MachineTier.ADVANCED.targetRpm()
                + (long) basic * MachineTier.BASIC.targetRpm();
        }

        private long demandSu() {
            return demandRpm() * 8L;
        }
    }
}
