package pitheguy.countycolor.metadata;

import com.badlogic.gdx.Gdx;

import java.io.*;
import java.util.*;

public class StateBorders {
    private static final String[] STATES = {
        "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
        "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
        "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
        "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
        "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
        "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
        "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia",
        "Wisconsin", "Wyoming"
    };

    private static final int SIZE = STATES.length;
    private static final boolean[][] BORDER_MATRIX = loadMatrix();

    public static List<String> getBorderingStates(String state) {
        int index = Arrays.asList(STATES).indexOf(state);
        if (index == -1) throw new IllegalArgumentException("Invalid state name: " + state);
        List<String> borderingStates = new ArrayList<>();
        for (int j = 0; j < SIZE; j++) if (BORDER_MATRIX[index][j]) borderingStates.add(STATES[j]);
        return borderingStates;
    }

    private static boolean[][] loadMatrix() {
        boolean[][] result = new boolean[SIZE][SIZE];
        String data = Gdx.files.internal("metadata/borders.txt").readString();
        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) for (int j = 0; j < SIZE; j++) result[i][j] = lines[i].charAt(j) != '0';
        return result;
    }
}
