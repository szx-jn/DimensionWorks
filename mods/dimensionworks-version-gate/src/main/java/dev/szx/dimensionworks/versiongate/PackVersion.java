package dev.szx.dimensionworks.versiongate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PackVersion(int major, int minor, int patch) implements Comparable<PackVersion> {
    private static final int MAX_FILE_BYTES = 64;
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");

    public PackVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("版本号不能为负数");
        }
    }

    public static PackVersion parse(String value) {
        if (value == null) throw new IllegalArgumentException("版本号为空");
        Matcher matcher = VERSION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("版本号必须使用 x.x.x 三段数字格式");
        }
        try {
            return new PackVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("版本号每段不能超过 2147483647", exception);
        }
    }

    /** Reads exactly <gameDirectory>/VERSION, without a fallback or cached version. */
    public static PackVersion readVersion(Path gameDirectory) throws IOException {
        Path file = gameDirectory.toAbsolutePath().normalize().resolve("VERSION");
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new IOException("VERSION 必须是普通文件");
            }
            byte[] bytes;
            try (var input = Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(MAX_FILE_BYTES + 1);
            }
            if (bytes.length > MAX_FILE_BYTES) throw new IOException("VERSION 超过 64 字节");
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("\ufeff")) text = text.substring(1);
            text = text.replaceAll("^[ \\t\\r\\n]+|[ \\t\\r\\n]+$", "");
            return parse(text);
        } catch (CharacterCodingException exception) {
            throw new IOException("版本文件 " + file + " 必须使用 UTF-8 编码", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IOException("无法读取版本文件 " + file + "：" + exception.getMessage(), exception);
        }
    }

    @Override
    public int compareTo(PackVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
