package pitheguy.countycolor.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import pitheguy.countycolor.render.util.RenderConst;

import java.util.BitSet;

public class ColoringGrid {
    private final Pixmap pixmap;
    private final BitSet bitSet;

    public ColoringGrid() {
        this(RenderConst.COLORING_SIZE);
    }

    private ColoringGrid(int gridSize) {
        pixmap = new Pixmap(gridSize, gridSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        pixmap.setColor(Color.CYAN);
        bitSet = new BitSet(gridSize * gridSize);
    }

    private ColoringGrid(Pixmap pixmap, BitSet bitSet) {
        this.pixmap = pixmap;
        this.bitSet = bitSet;
    }

    public static ColoringGrid fromBitSet(BitSet bitSet) {
        Pixmap pixmap = new Pixmap(RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(Color.CYAN);
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
            pixmap.drawPixel(i % RenderConst.COLORING_SIZE, RenderConst.COLORING_SIZE - i / RenderConst.COLORING_SIZE);
        return new ColoringGrid(pixmap, bitSet);
    }

    public Pixmap asPixmap() {
        return pixmap;
    }

    public BitSet asBitSet() {
        return bitSet;
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
