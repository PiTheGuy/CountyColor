package pitheguy.countycolor.coloring;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gson.JsonObject;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.BitSet;

public class ColoringGrid {
    private final Pixmap pixmap;
    private final BitSet bitSet;
    private final MapColor color;

    public ColoringGrid(MapColor color) {
        this(RenderConst.COLORING_SIZE, color);
    }

    private ColoringGrid(int gridSize, MapColor color) {
        this.color = color;
        pixmap = new Pixmap(gridSize, gridSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        pixmap.setColor(color.getColor());
        bitSet = new BitSet(gridSize * gridSize);
    }

    private ColoringGrid(Pixmap pixmap, BitSet bitSet, MapColor color) {
        this.pixmap = pixmap;
        this.bitSet = bitSet;
        this.color = color;
    }

    public static ColoringGrid fromJson(JsonObject json) {
        MapColor color = MapColor.fromSerializedName(json.get("color").getAsString());
        BitSet bitSet = BitSet.valueOf(Util.decompress(Base64.getDecoder().decode(json.get("coloredPoints").getAsString())));
        Pixmap pixmap = new Pixmap(RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(color.getColor());
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
            pixmap.drawPixel(i % RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE - i / RenderConst.COLORING_SIZE);
        return new ColoringGrid(pixmap, bitSet, color);
    }

    public Pixmap asPixmap() {
        return pixmap;
    }

    public BitSet asBitSet() {
        return bitSet;
    }

    public MapColor getColor() {
        return color;
    }

    public void set(int x, int y) {
        pixmap.drawPixel(x, RenderConst.COLORING_SIZE - y);
        bitSet.set(y * RenderConst.COLORING_SIZE + x);
    }

    public boolean get(int x, int y) {
        return bitSet.get(y * RenderConst.COLORING_SIZE + x);
    }

    public int coloredPoints() {
        return bitSet.cardinality();
    }
}
