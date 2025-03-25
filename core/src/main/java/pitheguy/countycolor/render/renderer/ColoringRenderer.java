package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.coloring.ColoringGrid;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class ColoringRenderer {
    private final Texture whitePixel = createWhiteTexture();
    private final SpriteBatch batch = new SpriteBatch();

    public ColoringRenderer() {
    }

    public void render(ColoringGrid grid, OrthographicCamera camera) {
        Texture texture = new Texture(grid.asPixmap());
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(texture, -RENDER_SIZE / 2f, -RENDER_SIZE / 2f, RENDER_SIZE, RENDER_SIZE);
        batch.end();
        texture.dispose();
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
