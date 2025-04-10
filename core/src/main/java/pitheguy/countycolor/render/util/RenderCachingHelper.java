package pitheguy.countycolor.render.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

import java.util.function.Consumer;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class RenderCachingHelper implements Disposable {
    private TextureRegion cachedTexture;
    private FrameBuffer frameBuffer;
    private final Batch batch = new SpriteBatch();
    private float cachedZoom = -1f;
    private float cachedViewportWidth = -1f;
    private float cachedViewportHeight = -1f;

    public void render(OrthographicCamera camera, Consumer<OrthographicCamera> renderer) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        if (useCachedTexture()) {
            if (cachedTexture == null) {
                frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
                frameBuffer.begin();
                renderer.accept(camera);
                Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                Texture texture = new Texture(pixmap);
                cachedTexture = new TextureRegion(texture);
                cachedTexture.flip(false, true);
                frameBuffer.end();
                pixmap.dispose();

                cachedZoom = camera.zoom;
                cachedViewportWidth = camera.viewportWidth;
                cachedViewportHeight = camera.viewportHeight;
            }
            batch.begin();
            float worldWidth = cachedViewportWidth * cachedZoom;
            float worldHeight = cachedViewportHeight * cachedZoom;
            batch.draw(cachedTexture, -worldWidth / 2f, -worldHeight / 2f, worldWidth, worldHeight);
            batch.end();

        } else renderer.accept(camera);
    }

    private boolean useCachedTexture() {
        return Gdx.graphics.getHeight() >= RENDER_SIZE &&
               Gdx.graphics.getWidth() >= RENDER_SIZE &&
               Gdx.graphics.getHeight() <= Gdx.graphics.getWidth();
    }

    public void invalidateCache() {
        if (cachedTexture != null) cachedTexture.getTexture().dispose();
        cachedTexture = null;
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
        if (cachedTexture != null) cachedTexture.getTexture().dispose();
    }
}
