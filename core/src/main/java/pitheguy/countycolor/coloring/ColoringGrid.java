package pitheguy.countycolor.coloring;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.BitSet;

public class ColoringGrid {
    private final Pixmap pixmap;
    private final BitSet bitSet;
    private MapColor color;

    public ColoringGrid() {
        this(RenderConst.COLORING_SIZE, null);
    }

    private ColoringGrid(int gridSize, MapColor color) {
        this.color = color;
        pixmap = new Pixmap(gridSize, gridSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        if (color != null) pixmap.setColor(color.getColor());
        bitSet = new BitSet(gridSize * gridSize);
    }

    private ColoringGrid(Pixmap pixmap, BitSet bitSet, MapColor color) {
        this.pixmap = pixmap;
        this.bitSet = bitSet;
        this.color = color;
    }

    public static ColoringGrid fromJson(JsonValue json) {
        MapColor color = MapColor.fromSerializedName(json.getString("color"));

        String encoded = json.getString("coloredPoints");
        byte[] compressed = Base64.getDecoder().decode(encoded);
        BitSet bitSet = BitSet.valueOf(Util.decompress(compressed));

        Pixmap pixmap = new Pixmap(RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        pixmap.setColor(color.getColor());
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            pixmap.drawPixel(i % RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE - i / RenderConst.COLORING_SIZE);
        }

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
        pixmap.drawPixel(x, RenderConst.COLORING_SIZE - y);
        bitSet.set(y * RenderConst.COLORING_SIZE + x);
    }

    public boolean get(int x, int y) {
        return bitSet.get(y * RenderConst.COLORING_SIZE + x);
    }

    public int coloredPoints() {
        return bitSet.cardinality();
    }

    public void dispose() {
        pixmap.dispose();
    }
}
