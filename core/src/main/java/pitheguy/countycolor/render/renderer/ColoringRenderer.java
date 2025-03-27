package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.coloring.ColoringGrid;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class ColoringRenderer {
    private final SpriteBatch batch = new SpriteBatch();
    private Texture cachedTexture;
    private int previousColoredPoints = 0;

    public void render(ColoringGrid grid, OrthographicCamera camera) {
        if (cachedTexture == null || grid.coloredPoints() != previousColoredPoints) {
            if (cachedTexture != null) cachedTexture.dispose();
            cachedTexture = new Texture(grid.asPixmap());
            previousColoredPoints = grid.coloredPoints();
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(cachedTexture, -RENDER_SIZE / 2f, -RENDER_SIZE / 2f, RENDER_SIZE, RENDER_SIZE);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
        if (cachedTexture != null) cachedTexture.dispose();
    }
}
