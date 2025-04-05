package pitheguy.countycolor.coloring;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_RESOLUTION;
import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class ColoringGrid implements Disposable {
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
        BitSet bitSet = BitSet.valueOf(Util.decompress(compressed));

        Pixmap pixmap = new Pixmap(COLORING_SIZE, COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        pixmap.setColor(color.getColor());
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
            pixmap.drawPixel(i % COLORING_SIZE, COLORING_SIZE - i / COLORING_SIZE);

        return new ColoringGrid(pixmap, bitSet, color);
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
