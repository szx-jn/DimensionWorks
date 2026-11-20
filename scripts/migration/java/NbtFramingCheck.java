package dev.szx.dimensionworks.migration;

import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/** 直接按原生 NbtIo 流式读取压缩文件；不用 JSON/base64 或 DFU 复制整个大批次。 */
public final class NbtFramingCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one prepared compressed NBT file");
        try (var input = new DataInputStream(new GZIPInputStream(Files.newInputStream(Path.of(args[0]))))) {
            NbtIo.read(input, NbtAccounter.unlimitedHeap());
            if (input.read() != -1) throw new IllegalStateException("Trailing prepared NBT data");
        }
        System.out.println("NATIVE_NBT_FRAMING_PASSED");
    }
}
