package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.coloring.ColoringGrid;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class ColoringRenderer {
    private final SpriteBatch batch = new SpriteBatch();
    private Texture cachedTexture;

    public void render(ColoringGrid grid, OrthographicCamera camera) {
        if (cachedTexture == null || grid.needsTextureUpdate()) {
            if (cachedTexture != null) cachedTexture.dispose();
            cachedTexture = new Texture(grid.asPixmap());
            grid.textureUpdated();
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
