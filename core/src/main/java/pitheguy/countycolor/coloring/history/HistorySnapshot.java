package pitheguy.countycolor.coloring.history;

import com.badlogic.gdx.graphics.*;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;

import java.util.BitSet;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class HistorySnapshot {
    private static final int DOWNSCALE_FACTOR = 4;
    private final BitSet bitSet;
    private Pixmap pixmap;
    private Texture texture;

    public HistorySnapshot(ColoringGrid grid) {
        this(grid.asBitSet(), grid.getColor());
    }

    public HistorySnapshot(BitSet bitSet, MapColor color) {
        this.bitSet = new BitSet(bitSet.length() / (DOWNSCALE_FACTOR * DOWNSCALE_FACTOR));
        for (int y = 0; y < COLORING_SIZE / DOWNSCALE_FACTOR; y++)
            for (int x = 0; x < COLORING_SIZE / DOWNSCALE_FACTOR; x++)
                if (bitSet.get(y * DOWNSCALE_FACTOR * COLORING_SIZE + x * DOWNSCALE_FACTOR))
                    this.bitSet.set(y * COLORING_SIZE + x);
        pixmap = new Pixmap(COLORING_SIZE / DOWNSCALE_FACTOR, COLORING_SIZE / DOWNSCALE_FACTOR, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixmap.setColor(color.getColor());
        for (int i = 0; i < this.bitSet.length(); i++)
            if (this.bitSet.get(i)) pixmap.drawPixel(i % (COLORING_SIZE / DOWNSCALE_FACTOR), (COLORING_SIZE / DOWNSCALE_FACTOR) - i / (COLORING_SIZE / DOWNSCALE_FACTOR));
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    public Texture getTexture() {
        if (texture == null) rasterize();
        return texture;
    }

    public void dispose() {
        if (pixmap != null) pixmap.dispose();
        if (texture != null) texture.dispose();
    }

    public void rasterize() {
        if (texture != null) return;
        texture = new Texture(pixmap);
        pixmap.dispose();
        pixmap = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof HistorySnapshot)) return false;
        HistorySnapshot other = (HistorySnapshot) obj;
        return bitSet.equals(other.bitSet);
    }
}
