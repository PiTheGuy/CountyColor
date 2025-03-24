package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.render.ColoringGrid;

import java.util.BitSet;

import static pitheguy.countycolor.render.util.RenderConst.*;

public class ColoringRenderer {
    private final Texture whitePixel = createWhiteTexture();
    private final SpriteBatch batch = new SpriteBatch();

    public ColoringRenderer() {
    }

    public void render(ColoringGrid grid, OrthographicCamera camera) {
        Vector2 camPos = new Vector2(camera.position.x, camera.position.y);
        float camRadius = RENDER_SIZE * camera.zoom / 2;
        float camMinX = camPos.x - camRadius;
        float camMinY = camPos.y - camRadius;
        float camMaxX = camPos.x + camRadius;
        float camMaxY = camPos.y + camRadius;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.CYAN);
        BitSet bits = grid.asBitSet();
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            int startIndex = i;
            float worldX = getWorldX(startIndex);
            float worldY = getWorldY(startIndex);
            if (worldX < camMinX || worldX > camMaxX || worldY < camMinY) continue;
            if (worldY > camMaxY) break;
            while (i + 1 < bits.length() && bits.get(i + 1) && i % COLORING_SIZE == worldY) i++;
            int totalCells = i - startIndex + 1;
            float width = (float) totalCells / COLORING_RESOLUTION;
            float height = 1f / COLORING_RESOLUTION;
            batch.draw(whitePixel, worldX, worldY, width, height);
        }
        batch.end();
    }

    private static float getWorldX(int index) {
        return (float) (index % COLORING_SIZE) / COLORING_RESOLUTION - RENDER_SIZE / 2f;
    }

    private static float getWorldY(int index) {
        return (float) (index / COLORING_SIZE) / COLORING_RESOLUTION - RENDER_SIZE / 2f;
    }

    public void dispose() {
        whitePixel.dispose();
        batch.dispose();
    }

    public static Texture createWhiteTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whitePixel = new Texture(pixmap);
        pixmap.dispose();
        return whitePixel;
    }
}
