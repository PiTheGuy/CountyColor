package pitheguy.countycolor.coloring.history;

import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.util.Util;

import java.io.*;
import java.util.*;

import static pitheguy.countycolor.render.util.RenderConst.COLORING_SIZE;

public class ColoringHistory {
    private final List<HistorySnapshot> snapshots = new ArrayList<>();

    public void addSnapshot(HistorySnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public List<HistorySnapshot> getSnapshots() {
        return snapshots;
    }

    public byte[] encode() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(stream);
        BitSet previousBits = new BitSet(COLORING_SIZE * COLORING_SIZE);
        for (HistorySnapshot snapshot : new ArrayList<>(snapshots)) {
            BitSet currentBits = snapshot.getBitSet();
            BitSet diffBits = (BitSet) currentBits.clone();
            diffBits.andNot(previousBits);
            int index = diffBits.nextSetBit(0);
            List<Integer> nums = new ArrayList<>();
            while (index != -1) {
                int end = diffBits.nextClearBit(index);
                int length = end - index;
                nums.add(index);
                nums.add(length);
                index = diffBits.nextSetBit(end + 1);
            }
            Util.writeVarInt(nums.size() / 2, stream);
            try {
                for (int num : nums) data.writeInt(num);
            } catch (IOException ignored) {}
            previousBits = currentBits;
        }
        return Util.compress(stream.toByteArray());
    }

    public static ColoringHistory decode(byte[] data, MapColor color) {
        ColoringHistory history = new ColoringHistory();
        ByteArrayInputStream stream = new ByteArrayInputStream(Util.decompress(data));
        DataInputStream dataStream = new DataInputStream(stream);
        BitSet currentBits = new BitSet(COLORING_SIZE * COLORING_SIZE);
        try {
            while (stream.available() > 0) {
                int numRecords = Util.readVarInt(stream);
                for (int i = 0; i < numRecords; i++) {
                    int index = dataStream.readInt();
                    int length = dataStream.readInt();
                    currentBits.set(index, index + length);
                }
                history.snapshots.add(new HistorySnapshot((BitSet) currentBits.clone(), color));
            }
        } catch (IOException ignored) {}
        return history;
    }

    public void dispose() {
        for (HistorySnapshot snapshot : snapshots) snapshot.dispose();
    }
}
