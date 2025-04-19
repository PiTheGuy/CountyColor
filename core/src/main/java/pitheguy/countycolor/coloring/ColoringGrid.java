package pitheguy.countycolor.coloring;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.util.Util;

import java.io.*;
import java.util.Base64;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_RESOLUTION;
import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class ColoringGrid implements Disposable {
    private static final int BLOCK_SIZE = 100;
    private final Pixmap pixmap;
    private final BitSet bitSet;
    private MapColor color;
    private final ExecutorService pixmapUpdateExecutor;
    private boolean needsTextureUpdate = false;

    public ColoringGrid() {
        this.color = null;
        pixmap = new Pixmap(COLORING_SIZE, COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        bitSet = new BitSet(COLORING_SIZE * COLORING_SIZE);
        pixmapUpdateExecutor = Executors.newSingleThreadExecutor();
    }

    private ColoringGrid(Pixmap pixmap, BitSet bitSet, MapColor color) {
        this.pixmap = pixmap;
        this.bitSet = bitSet;
        this.color = color;
        pixmapUpdateExecutor = Executors.newSingleThreadExecutor();
    }

    public static ColoringGrid fromJson(JsonValue json) {
        MapColor color = MapColor.fromSerializedName(json.getString("color"));

        String encoded = json.getString("coloredPoints");
        byte[] compressed = Base64.getDecoder().decode(encoded);
        BitSet bitSet = decode(compressed);

        Pixmap pixmap = new Pixmap(COLORING_SIZE, COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        pixmap.setColor(color.getColor());
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
            pixmap.drawPixel(i % COLORING_SIZE, COLORING_SIZE - i / COLORING_SIZE);

        return new ColoringGrid(pixmap, bitSet, color);
    }

    private byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        //noinspection ConstantValue
        assert COLORING_SIZE % BLOCK_SIZE == 0;
        for (int blockY = 0; blockY < COLORING_SIZE / BLOCK_SIZE; blockY++) {
            for (int blockX = 0; blockX < COLORING_SIZE / BLOCK_SIZE; blockX++) {
                boolean start = get(blockX * BLOCK_SIZE, blockY * BLOCK_SIZE);
                boolean allSame = true;
                loop:
                for (int x = 0; x < BLOCK_SIZE; x++) {
                    for (int y = 0; y < BLOCK_SIZE; y++) {
                        if (get(blockX * BLOCK_SIZE + x, blockY * BLOCK_SIZE + y) != start) {
                            allSame = false;
                            break loop;
                        }
                    }
                }
                try {
                    if (allSame) {
                        dos.writeByte(start ? 2 : 1);
                    } else {
                        dos.writeByte(0);
                        ByteArrayOutputStream rleStream = new ByteArrayOutputStream();
                        DataOutputStream rleDataStream = new DataOutputStream(rleStream);
                        boolean current = get(blockX * BLOCK_SIZE, blockY * BLOCK_SIZE);
                        int runLength = 0;
                        if (current) writeVarInt(0, rleDataStream);

                        for (int y = 0; y < BLOCK_SIZE; y++) {
                            for (int x = 0; x < BLOCK_SIZE; x++) {
                                if (get(blockX * BLOCK_SIZE + x, blockY * BLOCK_SIZE + y) != current) {
                                    writeVarInt(runLength, rleDataStream);
                                    current = get(blockX * BLOCK_SIZE + x, blockY * BLOCK_SIZE + y);
                                    runLength = 1;
                                } else runLength++;
                            }
                        }
                        writeVarInt(runLength, rleDataStream);
                        byte[] rleBytes = rleStream.toByteArray();
                        if (rleBytes[rleBytes.length - 1] == 0) System.out.println("Last byte was zero");
                        dos.writeShort(rleBytes.length);
                        dos.write(rleBytes);
                    }
                } catch (IOException ignored) {}
            }
        }
        return Util.compress(baos.toByteArray());
    }

    private static BitSet decode(byte[] input) {
        ByteArrayInputStream bais = new ByteArrayInputStream(Util.decompress(input));
        DataInputStream dis = new DataInputStream(bais);
        BitSet bitSet = new BitSet(COLORING_SIZE * COLORING_SIZE);
        for (int blockY = 0; blockY < COLORING_SIZE / BLOCK_SIZE; blockY++) {
            for (int blockX = 0; blockX < COLORING_SIZE / BLOCK_SIZE; blockX++) {
                try {
                    byte header = dis.readByte();
                    if (header == 2) for (int y = 0; y < BLOCK_SIZE; y++) {
                        int bitSetY = blockY * BLOCK_SIZE + y;
                        int startX = blockX * BLOCK_SIZE;
                        bitSet.set(bitSetY * COLORING_SIZE + startX, bitSetY * COLORING_SIZE + startX + BLOCK_SIZE);
                    }
                    else if (header == 0) {
                        int size = dis.readShort();
                        byte[] rleBytes = dis.readNBytes(size);
                        ByteArrayInputStream rleBais = new ByteArrayInputStream(rleBytes);
                        DataInputStream rleDis = new DataInputStream(rleBais);
                        int index = 0;
                        boolean current = false;
                        while (rleDis.available() > 0) {
                            int runLength = readVarInt(rleDis);
                            if (current) {
                                for (int i = index; i < index + runLength; i++) {
                                    int bitSetY = blockY * BLOCK_SIZE + i / BLOCK_SIZE;
                                    int bitSetX = blockX * BLOCK_SIZE + i % BLOCK_SIZE;
                                    bitSet.set(bitSetY * COLORING_SIZE + bitSetX);
                                }
                            }
                            index += runLength;
                            current = !current;
                        }
                    } else if (header != 1) throw new RuntimeException("Invalid header byte: " + header);
                } catch (IOException ignored) {}
            }
        }
        return bitSet;
    }

    private static void writeVarInt(int value, OutputStream out) {
        try {
            while ((value & 0xFFFFFF80) != 0L) {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            out.write(value & 0x7F);
        } catch (IOException ignored) {}
    }

    private static int readVarInt(InputStream in) {
        int value = 0;
        int position = 0;
        int b;
        try {
            while (((b = in.read()) & 0x80) != 0) {
                value |= (b & 0x7F) << position;
                position += 7;
            }
            value |= b << position;
        } catch (IOException ignored) {}
        return value;
    }

    public void setColor(MapColor color) {
        this.color = color;
        pixmap.setColor(color.getColor());
    }

    public Pixmap asPixmap() {
        return pixmap;
    }

    public BitSet asBitSet() {
        return bitSet;
    }

    public String asEncodedString() {
        return Base64.getEncoder().encodeToString(encode());
    }

    public MapColor getColor() {
        if (color == null) throw new IllegalStateException("Color hasn't been set yet");
        return color;
    }

    public void set(int x, int y) {
        pixmap.drawPixel(x, COLORING_SIZE - y);
        bitSet.set(y * COLORING_SIZE + x);
    }

    public void applyBrush(Vector2 pos, float brushSize) {
        int effectiveBrushSize = (int) (brushSize * COLORING_RESOLUTION);
        int centerX = (int) (pos.x * COLORING_RESOLUTION + COLORING_SIZE / 2f);
        int centerY = (int) (pos.y * COLORING_RESOLUTION + COLORING_SIZE / 2f);
        if (Options.ASYNC_GRID_UPDATES.get()) pixmapUpdateExecutor.submit(() -> fillPixmapCircle(centerX, centerY, effectiveBrushSize));
        else fillPixmapCircle(centerX, centerY, effectiveBrushSize);
        int startX = (int) (pos.x * COLORING_RESOLUTION - effectiveBrushSize);
        int startY = (int) (pos.y * COLORING_RESOLUTION - effectiveBrushSize);
        int endX = (int) (pos.x * COLORING_RESOLUTION + effectiveBrushSize);
        int endY = (int) (pos.y * COLORING_RESOLUTION + effectiveBrushSize);
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                float dx = pos.x * COLORING_RESOLUTION - x;
                float dy = pos.y * COLORING_RESOLUTION - y;
                if (dx * dx + dy * dy < effectiveBrushSize * effectiveBrushSize) {
                    int indexX = x + COLORING_SIZE / 2;
                    int indexY = y + COLORING_SIZE / 2;
                    bitSet.set(indexY * COLORING_SIZE + indexX);
                }
            }
        }
    }

    private void fillPixmapCircle(int centerX, int centerY, int effectiveBrushSize) {
        pixmap.fillCircle(centerX, COLORING_SIZE - centerY, effectiveBrushSize);
        needsTextureUpdate = true;
    }

    public boolean get(int x, int y) {
        return bitSet.get(y * COLORING_SIZE + x);
    }

    public int coloredPoints() {
        return bitSet.cardinality();
    }

    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    public boolean needsTextureUpdate() {
        return needsTextureUpdate;
    }

    public void textureUpdated() {
        needsTextureUpdate = false;
    }

    public void dispose() {
        pixmap.dispose();
        pixmapUpdateExecutor.shutdownNow();
    }
}
