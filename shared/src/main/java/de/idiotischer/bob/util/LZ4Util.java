package de.idiotischer.bob.util;

import net.jpountz.lz4.*;

import java.util.Arrays;

public class LZ4Util {

    private static final LZ4Factory factory = LZ4Factory.fastestInstance();

    public static byte[] compress(byte[] data) {
        LZ4Compressor compressor = factory.fastCompressor();
        int maxSize = compressor.maxCompressedLength(data.length);

        byte[] compressed = new byte[maxSize];
        int size = compressor.compress(data, 0, data.length, compressed, 0);

        return Arrays.copyOf(compressed, size);
    }

    public static byte[] decompress(byte[] data, int originalSize) {
        LZ4FastDecompressor decompressor = factory.fastDecompressor();

        byte[] restored = new byte[originalSize];
        decompressor.decompress(data, 0, restored, 0, originalSize);

        return restored;
    }
}