package pitheguy.countycolor.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test
    public void testCompression() {
        byte[] data = new byte[1000000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        byte[] compressed = Util.compress(data);
        byte[] decompressed = Util.decompress(compressed);
        assertArrayEquals(data, decompressed);
    }

    @Test
    public void testVarIntEncoding() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int valueToWrite = 123456;
        Util.writeVarInt(valueToWrite, byteArrayOutputStream);

        byte[] encodedBytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedBytes);
        int decodedValue = Util.readVarInt(byteArrayInputStream);
        assertEquals(valueToWrite, decodedValue);
    }
}
