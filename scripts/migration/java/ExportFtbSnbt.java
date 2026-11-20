package dev.szx.dimensionworks.migration;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.NbtIo;

/** 只调用 FTB 的解析器；不复制其代码，也不写回原始 SNBT。 */
public final class ExportFtbSnbt {
    private static String digest(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("参数：原存档目录 独立缓存目录");
        Path world = Path.of(args[0]).toRealPath();
        Path cache = Path.of(args[1]).toAbsolutePath().normalize();
        if (cache.startsWith(world) || Files.exists(cache)) {
            throw new IllegalArgumentException("缓存必须在存档外的新目录，拒绝覆盖");
        }
        Files.createDirectories(cache);
        Map<String, Map<String, String>> sources = new LinkedHashMap<>();
        try (var files = Files.walk(world)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".snbt"))
                    .filter(path -> !world.relativize(path).startsWith("trueuuid-backups"))
                    .sorted().toList()) {
                String relative = world.relativize(file).toString().replace('\\', '/');
                String before = digest(file);
                var data = SNBT.tryRead(file);
                if (data == null) throw new IllegalStateException("FTB 解析失败：" + relative);
                Path target = cache.resolve(relative + ".nbt");
                Files.createDirectories(target.getParent());
                NbtIo.writeCompressed(data, target);
                if (!before.equals(digest(file))) throw new IllegalStateException("读取时源文件变化：" + relative);
                sources.put(relative, Map.of("source_sha256", before, "nbt_sha256", digest(target)));
            }
        }
        Files.writeString(cache.resolve("sources.json"), new GsonBuilder().setPrettyPrinting().create().toJson(sources),
                StandardCharsets.UTF_8);
        System.out.println("FTB SNBT records exported: " + sources.size());
    }
}
