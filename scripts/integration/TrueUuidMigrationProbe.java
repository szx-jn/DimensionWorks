import cn.alini.trueuuid.protocol.MigrationExecutor;
import cn.alini.trueuuid.protocol.MigrationPlanner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 用已发布的 TrueUUID JAR 验证文件迁移，不启动 Minecraft，也不访问实际存档。
 * Java 17: java -cp /path/to/trueuuid.jar scripts/integration/TrueUuidMigrationProbe.java
 */
public final class TrueUuidMigrationProbe {
    private record FileCase(String directory, String suffix, String group, boolean rewrite) {}
    private record Expected(Path source, Path target, Path sourceBackup, Path targetBackup,
                            byte[] sourceBytes, byte[] targetBytes, byte[] previousTargetBytes) {}

    private static final List<FileCase> FILES = List.of(
            new FileCase("playerdata", ".dat", "vanilla", false),
            new FileCase("playerdata", ".dat_old", "vanilla", false),
            new FileCase("advancements", ".json", "vanilla/advancements", false),
            new FileCase("stats", ".json", "vanilla", false),
            new FileCase("ftbchunks", ".snbt", "ftbchunks", true),
            new FileCase("ftbessentials/playerdata", ".snbt", "ftbessentials", true),
            new FileCase("ftbteams/player", ".snbt", "ftbteams", true),
            new FileCase("ftbquests", ".snbt", "ftbquests", true));

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("dw-trueuuid-migration-probe-");
        try {
            run(root);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: 8 UUID files, separate backups, source cleanup, global references, repeated migration");
    }

    private static void run(Path root) throws Exception {
        UUID from = MigrationExecutor.offlineUuid("DimensionWorksProbe");
        UUID to = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Path world = root.resolve("world");
        Path backup = root.resolve("backup");
        List<MigrationPlanner.Candidate> candidates = new ArrayList<>();
        List<Expected> expected = new ArrayList<>();

        for (FileCase file : FILES) {
            String label = file.directory() + file.suffix();
            Path source = world.resolve(file.directory()).resolve(from + file.suffix());
            Path target = world.resolve(file.directory()).resolve(to + file.suffix());
            byte[] sourceBytes = bytes(payload(label, from));
            byte[] previousTarget = bytes("previous verified data: " + label);
            write(source, sourceBytes);
            write(target, previousTarget);
            candidates.add(new MigrationPlanner.Candidate(source, target,
                    file.rewrite() ? MigrationPlanner.Mode.UUID_TEXT_REWRITE : MigrationPlanner.Mode.BINARY_COPY,
                    file.group()));
            expected.add(new Expected(source, target,
                    backup.resolve("offline").resolve(file.group()).resolve(source.getFileName()),
                    backup.resolve("verified-existing").resolve(file.group()).resolve(target.getFileName()),
                    sourceBytes, file.rewrite() ? bytes(payload(label, to)) : sourceBytes, previousTarget));
        }

        Path global = world.resolve("serverconfig/ftbranks/players.snbt");
        String beforeGlobal = "owner=" + from + "\ncompact=" + compact(from) + "\nother-player=keep\n";
        write(global, bytes(beforeGlobal));
        MigrationPlanner.Plan plan = MigrationPlanner.preflight(candidates, Files::exists);
        require(plan.operations().size() == 8, "preflight omitted a UUID file");
        MigrationExecutor.execute(plan, backup, from, to, global);

        for (Expected file : expected) {
            require(!Files.exists(file.source()), "offline source remains: " + file.source());
            equal(file.target(), file.targetBytes());
            equal(file.sourceBackup(), file.sourceBytes());
            equal(file.targetBackup(), file.previousTargetBytes());
        }
        equal(global, bytes("owner=" + to + "\ncompact=" + compact(to) + "\nother-player=keep\n"));
        equal(backup.resolve("global/players.snbt"), bytes(beforeGlobal));
        require(Files.readString(backup.resolve("journal.log")).endsWith("COMMITTED\n"), "migration did not commit");

        // 模拟玩家迁移后产生新进度，再次迁移不得用旧副本覆盖它。
        Path playedTarget = expected.get(0).target();
        byte[] playedData = bytes("verified progress after playing");
        write(playedTarget, playedData);
        MigrationPlanner.Plan repeated = MigrationPlanner.preflight(candidates, Files::exists);
        require(repeated.isEmpty(), "completed migration still discovers offline sources");
        MigrationExecutor.execute(repeated, root.resolve("repeated-backup"), from, to, global);
        equal(playedTarget, playedData);
    }

    private static String payload(String label, UUID owner) {
        return label + " owner=" + owner + " compact=" + compact(owner);
    }

    private static String compact(UUID uuid) { return uuid.toString().replace("-", ""); }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void write(Path path, byte[] data) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }
    private static void equal(Path path, byte[] expected) throws Exception {
        require(Arrays.equals(Files.readAllBytes(path), expected), "data differs: " + path);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
