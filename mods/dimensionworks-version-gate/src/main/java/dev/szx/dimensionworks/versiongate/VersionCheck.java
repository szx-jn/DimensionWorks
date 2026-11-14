package dev.szx.dimensionworks.versiongate;

import java.io.IOException;
import java.nio.file.Path;

/** A login decision whose local version can only come from the game's VERSION file. */
public record VersionCheck(Status status, PackVersion local, PackVersion server, String error) {
    public enum Status {
        CURRENT, TOO_OLD, TOO_NEW, LOCAL_VERSION_ERROR, SERVER_VERSION_ERROR
    }

    public static VersionCheck evaluate(Path gameDirectory, String serverText) {
        PackVersion local = null;
        PackVersion server = null;
        String localError = null;
        try {
            local = PackVersion.readVersion(gameDirectory);
        } catch (IOException exception) {
            localError = exception.getMessage();
        }
        try {
            server = PackVersion.parse(serverText);
        } catch (IllegalArgumentException exception) {
            if (localError == null) {
                return new VersionCheck(Status.SERVER_VERSION_ERROR, local, null, exception.getMessage());
            }
        }
        if (localError != null) {
            return new VersionCheck(Status.LOCAL_VERSION_ERROR, null, server, localError);
        }
        int comparison = local.compareTo(server);
        return new VersionCheck(comparison == 0 ? Status.CURRENT : comparison < 0 ? Status.TOO_OLD : Status.TOO_NEW,
                local, server, "");
    }

    public String localText() {
        return local == null ? "无法读取" : local.toString();
    }

    public String serverText() {
        return server == null ? "无法读取" : server.toString();
    }

    public String message() {
        return switch (status) {
            case CURRENT -> "整合包版本检查通过";
            case TOO_OLD -> "整合包版本过低，无法加入服务器";
            case TOO_NEW -> "整合包版本高于服务器，无法加入服务器";
            case LOCAL_VERSION_ERROR -> "无法读取客户端 VERSION，无法加入服务器";
            case SERVER_VERSION_ERROR -> "服务器 VERSION 无效，无法加入服务器";
        };
    }
}
