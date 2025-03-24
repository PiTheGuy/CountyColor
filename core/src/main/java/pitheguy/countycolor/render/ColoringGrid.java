package pitheguy.countycolor.render;

import pitheguy.countycolor.render.util.RenderConst;

import java.util.BitSet;

public class ColoringGrid {
    public static final int DOWNSIZE_FACTOR = 2;
    private final BitSet filled;
    private final BitSet downsized;

    public ColoringGrid() {
        this(RenderConst.COLORING_SIZE);
    }

    private ColoringGrid(int gridSize) {
        filled = new BitSet(gridSize * gridSize);
        downsized = new BitSet(gridSize / DOWNSIZE_FACTOR * gridSize / DOWNSIZE_FACTOR);
    }

    private ColoringGrid(BitSet filled) {
        this.filled = filled;
        this.downsized = downsize(filled);
    }

    public static ColoringGrid fromBitSet(BitSet bitSet) {
        return new ColoringGrid(bitSet);
    }

    public BitSet asBitSet() {
        return filled;
    }

    public void set(int x, int y) {
        filled.set(y * RenderConst.COLORING_SIZE + x);
        int downsizedX = x / DOWNSIZE_FACTOR;
        int downsizedY = y / DOWNSIZE_FACTOR;
        int count = 0;
        for (int dx = downsizedX * DOWNSIZE_FACTOR; dx < (downsizedX + 1) * DOWNSIZE_FACTOR; dx++) {
            for (int dy = downsizedY * DOWNSIZE_FACTOR; dy < (downsizedY + 1) * DOWNSIZE_FACTOR; dy++) {
                if (get(dx, dy)) count++;
            }
        }
        if (count >= DOWNSIZE_FACTOR * DOWNSIZE_FACTOR / 2) {
            int index = downsizedY * (RenderConst.COLORING_SIZE / DOWNSIZE_FACTOR) + downsizedX;
            downsized.set(index);
        }
    }

    public boolean get(int x, int y) {
        return filled.get(y * RenderConst.COLORING_SIZE + x);
    }

    public BitSet getDownsized() {
        return downsized;
    }

    private static BitSet downsize(BitSet bitSet) {
        int newSize = RenderConst.COLORING_SIZE / DOWNSIZE_FACTOR;
        int requiredBits = DOWNSIZE_FACTOR * DOWNSIZE_FACTOR / 2;
        BitSet newBitSet = new BitSet(newSize);
        for (int x = 0; x < newSize; x++) {
            for (int y = 0; y < newSize; y++) {
                int count = 0;
                for (int dx = 0; dx < DOWNSIZE_FACTOR; dx++)
                    for (int dy = 0; dy < DOWNSIZE_FACTOR; dy++)
                        if (bitSet.get((y * DOWNSIZE_FACTOR + dy) * RenderConst.COLORING_SIZE + (x * DOWNSIZE_FACTOR + dx))) count++;
                if (count >= requiredBits) newBitSet.set(y * newSize + x);
            }
        }
        return newBitSet;
    }

    public int coloredPoints() {
        return filled.cardinality();
    }
}
