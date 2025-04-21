package pitheguy.countycolor.coloring.history;

import com.badlogic.gdx.graphics.*;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;

import java.util.BitSet;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class HistorySnapshot {
    public static final int DOWNSCALE_FACTOR = 4;
    public static final int DOWNSCALED_SIZE = COLORING_SIZE / DOWNSCALE_FACTOR;
    private final BitSet bitSet;
    private Pixmap pixmap;
    private Texture texture;

    public HistorySnapshot(ColoringGrid grid) {
        this.bitSet = new BitSet(DOWNSCALED_SIZE * DOWNSCALED_SIZE);
        for (int y = 0; y < DOWNSCALED_SIZE; y++)
            for (int x = 0; x < DOWNSCALED_SIZE; x++)
                if (grid.get(x * DOWNSCALE_FACTOR, y * DOWNSCALE_FACTOR))
                    bitSet.set(y * DOWNSCALED_SIZE + x);
        pixmap = new Pixmap(DOWNSCALED_SIZE, DOWNSCALED_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixmap.setColor(grid.getColor().getColor());
        for (int i = 0; i < this.bitSet.length(); i++)
            if (this.bitSet.get(i)) pixmap.drawPixel(i % DOWNSCALED_SIZE, DOWNSCALED_SIZE - i / DOWNSCALED_SIZE);
    }

    public HistorySnapshot(BitSet bitSet, MapColor color) {
        this.bitSet = bitSet;
        pixmap = new Pixmap(DOWNSCALED_SIZE, DOWNSCALED_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixmap.setColor(color.getColor());
        for (int i = 0; i < this.bitSet.length(); i++)
            if (this.bitSet.get(i)) pixmap.drawPixel(i % DOWNSCALED_SIZE, DOWNSCALED_SIZE - i / DOWNSCALED_SIZE);
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

    public boolean isRasterized() {
        return texture != null;
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
