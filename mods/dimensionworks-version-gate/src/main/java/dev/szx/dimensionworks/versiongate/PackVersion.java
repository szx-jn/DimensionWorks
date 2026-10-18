package dev.szx.dimensionworks.versiongate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.fml.loading.FMLPaths;

public record PackVersion(int major, int minor, int patch) implements Comparable<PackVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    public static final PackVersion UNKNOWN = new PackVersion(0, 0, 0);

    public static PackVersion parse(String value) {
        if (value == null) return UNKNOWN;
        Matcher matcher = VERSION_PATTERN.matcher(value.trim());
        if (!matcher.matches()) return UNKNOWN;
        try {
            return new PackVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException ignored) {
            return UNKNOWN;
        }
    }

    public static String readLocalVersion() {
        Path file = FMLPaths.GAMEDIR.get().resolve("VERSION");
        try {
            if (Files.isRegularFile(file)) {
                String value = Files.readString(file).trim();
                if (VERSION_PATTERN.matcher(value).matches()) return value;
            }
        } catch (IOException ignored) {
            // A missing version file is treated as an old/unmanaged installation.
        }
        return "0.0.0";
    }

    public static String readPackVersion() {
        return readLocalVersion();
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
