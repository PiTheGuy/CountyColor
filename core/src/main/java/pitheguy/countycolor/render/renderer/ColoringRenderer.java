package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.coloring.ColoringGrid;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class ColoringRenderer {
    private final SpriteBatch batch = new SpriteBatch();

    public void render(ColoringGrid grid, OrthographicCamera camera) {
        Texture texture = new Texture(grid.asPixmap());
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(texture, -RENDER_SIZE / 2f, -RENDER_SIZE / 2f, RENDER_SIZE, RENDER_SIZE);
        batch.end();
        texture.dispose();
    }

    public void dispose() {
        batch.dispose();
    }
}
