package pitheguy.countycolor.util;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Util {
    public static byte[] compress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress data", e);
        }
    }

    public static byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(gzip.readAllBytes());
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress data", e);
        }
    }

    public static String getMemoryUsageString() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
        long totalMemoryMB = totalMemory / (1024 * 1024);
        long usedMemoryMB = usedMemory / (1024 * 1024);
        float percent = (float) usedMemory / totalMemory * 100;
        return String.format("Memory: %d MB / %d MB (%.2f%%)", usedMemoryMB, totalMemoryMB, percent);
    }

    public static <T> T getFutureValue(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
