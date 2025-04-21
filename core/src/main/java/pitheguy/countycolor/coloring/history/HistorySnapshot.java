package pitheguy.countycolor.coloring.history;

import com.badlogic.gdx.graphics.*;
import org.w3c.dom.Text;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;

import java.util.BitSet;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class HistorySnapshot {
    private final BitSet bitSet;
    private Pixmap pixmap;
    private Texture texture;

    public HistorySnapshot(ColoringGrid grid) {
        bitSet = grid.asBitSet();
        pixmap = grid.asPixmap();
    }

    public HistorySnapshot(BitSet bitSet, MapColor color) {
        this.bitSet = bitSet;
        pixmap = new Pixmap(COLORING_SIZE, COLORING_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixmap.setColor(color.getColor());
        for (int i = 0; i < bitSet.length(); i++)
            if (bitSet.get(i)) pixmap.drawPixel(i % COLORING_SIZE, COLORING_SIZE - i / COLORING_SIZE);
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
